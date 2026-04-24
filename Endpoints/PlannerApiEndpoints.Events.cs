using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Localization;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static IResult GetEvents(HttpRequest request, PlannerStore store, AppLocalizationService localization)
    {
        var pack = localization.ResolveLanguage(request.HttpContext);
        if (request.Query.TryGetValue("upcoming", out var upcomingValue) && upcomingValue == "1")
        {
            return Results.Ok(store
                .GetUpcomingEvents(DateOnly.FromDateTime(DateTime.Now))
                .Select(plannerEvent => localization.LocalizeGeneratedEvent(pack, plannerEvent, store))
                .ToList());
        }

        if (!DateOnly.TryParse(request.Query["start"], out var start) ||
            !DateOnly.TryParse(request.Query["end"], out var end))
        {
            return BadRequest(request.HttpContext, localization, "errors.events.invalid_date_range");
        }

        return Results.Ok(store
            .GetEvents(start, end)
            .Select(plannerEvent => localization.LocalizeGeneratedEvent(pack, plannerEvent, store))
            .ToList());
    }

    private static async Task<IResult> PostEventAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions, AppLocalizationService localization)
    {
        if (HasJsonContentType(request))
        {
            var command = await request.ReadFromJsonAsync<DeleteRequest>(jsonOptions.Value.SerializerOptions);
            if (command?.Delete == true)
            {
                if (command.Id <= 0)
                {
                    return BadRequest(request.HttpContext, localization, "errors.events.invalid_event");
                }

                store.DeleteEvent(command.Id);
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var title = Required(form["title"]);
        if (title is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.events.title_required");
        }

        var eventDate = Required(form["event_date"]);
        if (eventDate is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.events.date_required");
        }

        if (!DateOnly.TryParse(eventDate, out var eventDateValue))
        {
            return BadRequest(request.HttpContext, localization, "errors.events.invalid_date");
        }

        var rawRecurrenceType = form["recurrence_type"].ToString();
        var recurrenceType = NormalizeRecurrenceType(rawRecurrenceType);
        if (!string.IsNullOrWhiteSpace(rawRecurrenceType) && recurrenceType is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.events.invalid_recurrence");
        }

        var recurrenceUntil = string.IsNullOrWhiteSpace(form["recurrence_until"])
            ? null
            : form["recurrence_until"].ToString().Trim();
        if (recurrenceType is null && recurrenceUntil is not null)
        {
            return BadRequest(request.HttpContext, localization, "errors.events.recurrence_until_requires_recurrence");
        }

        if (recurrenceUntil is not null)
        {
            if (!DateOnly.TryParse(recurrenceUntil, out var recurrenceUntilDate))
            {
                return BadRequest(request.HttpContext, localization, "errors.events.invalid_recurrence_until");
            }

            if (recurrenceUntilDate < eventDateValue)
            {
                return BadRequest(request.HttpContext, localization, "errors.events.recurrence_until_before_start");
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
