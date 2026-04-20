using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static IResult GetEvents(HttpRequest request, PlannerStore store)
    {
        if (request.Query.TryGetValue("upcoming", out var upcomingValue) && upcomingValue == "1")
        {
            return Results.Ok(store.GetUpcomingEvents(DateOnly.FromDateTime(DateTime.Now)));
        }

        if (!DateOnly.TryParse(request.Query["start"], out var start) ||
            !DateOnly.TryParse(request.Query["end"], out var end))
        {
            return Results.BadRequest(new { error = "Ugyldig datointervall." });
        }

        return Results.Ok(store.GetEvents(start, end));
    }

    private static async Task<IResult> PostEventAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions)
    {
        if (HasJsonContentType(request))
        {
            var command = await request.ReadFromJsonAsync<DeleteRequest>(jsonOptions.Value.SerializerOptions);
            if (command?.Delete == true)
            {
                if (command.Id <= 0)
                {
                    return BadRequest("Ugyldig avtale.");
                }

                store.DeleteEvent(command.Id);
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var title = Required(form["title"], "Tittel mangler.");
        var eventDate = Required(form["event_date"], "Dato mangler.");
        if (!DateOnly.TryParse(eventDate, out var eventDateValue))
        {
            return BadRequest("Ugyldig dato.");
        }

        var rawRecurrenceType = form["recurrence_type"].ToString();
        var recurrenceType = NormalizeRecurrenceType(rawRecurrenceType);
        if (!string.IsNullOrWhiteSpace(rawRecurrenceType) && recurrenceType is null)
        {
            return BadRequest("Ugyldig gjentakelse.");
        }

        var recurrenceUntil = string.IsNullOrWhiteSpace(form["recurrence_until"])
            ? null
            : form["recurrence_until"].ToString().Trim();
        if (recurrenceType is null && recurrenceUntil is not null)
        {
            return BadRequest("Sluttdato krever gjentakelse.");
        }

        if (recurrenceUntil is not null)
        {
            if (!DateOnly.TryParse(recurrenceUntil, out var recurrenceUntilDate))
            {
                return BadRequest("Ugyldig sluttdato.");
            }

            if (recurrenceUntilDate < eventDateValue)
            {
                return BadRequest("Sluttdato kan ikke være før startdato.");
            }
        }

        store.UpsertEvent(
            ParseNullableInt(form["id"]),
            title,
            eventDate,
            form["start_time"],
            form["end_time"],
            recurrenceType,
            recurrenceUntil,
            ParseNullableInt(form["owner_id"]),
            form["color"],
            form["note"]);

        return Results.Ok(new { ok = true });
    }

    private static string? NormalizeRecurrenceType(string? value) =>
        string.IsNullOrWhiteSpace(value)
            ? null
            : value.Trim().ToLowerInvariant() switch
            {
                "daily" => "daily",
                "weekly" => "weekly",
                _ => null
            };
}
