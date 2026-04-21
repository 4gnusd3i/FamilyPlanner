using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    public static void MapPlannerApiEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var api = endpoints.MapGroup("/api");

        api.MapGet("/events", GetEvents);
        api.MapPost("/events", PostEventAsync);

        api.MapGet("/meals", (PlannerStore store) => Results.Ok(store.GetMeals()));
        api.MapPost("/meals", PostMealAsync);

        api.MapGet("/budget", (PlannerStore store) => Results.Ok(store.GetBudgetSnapshot()));
        api.MapPost("/budget", PostBudgetAsync);

        api.MapGet("/family", GetFamily);
        api.MapPost("/family", PostFamilyAsync);

        api.MapGet("/notes", (PlannerStore store) => Results.Ok(store.GetNotes()));
        api.MapPost("/notes", PostNotesAsync);

        api.MapGet("/shopping", (PlannerStore store) => Results.Ok(store.GetShoppingItems()));
        api.MapPost("/shopping", PostShoppingAsync);
    }

    private static bool HasJsonContentType(HttpRequest request) =>
        request.ContentType?.StartsWith("application/json", StringComparison.OrdinalIgnoreCase) == true;

    private static async Task<JsonElement?> ReadJsonObjectAsync(HttpRequest request, JsonOptions jsonOptions)
    {
        var body = await request.ReadFromJsonAsync<JsonElement>(jsonOptions.SerializerOptions);
        return body.ValueKind == JsonValueKind.Object ? body : null;
    }

    private static IResult BadRequest(string message) => Results.BadRequest(new { error = message });

    private static bool HasTrueProperty(JsonElement element, string propertyName) =>
        element.TryGetProperty(propertyName, out var property) &&
        property.ValueKind is JsonValueKind.True or JsonValueKind.False &&
        property.GetBoolean();

    private static bool TryGetRequiredInt(JsonElement element, string propertyName, out int value)
    {
        value = 0;
        if (!element.TryGetProperty(propertyName, out var property))
        {
            return false;
        }

        if (property.ValueKind == JsonValueKind.Number && property.TryGetInt32(out value))
        {
            return value > 0;
        }

        return property.ValueKind == JsonValueKind.String &&
               int.TryParse(property.GetString(), out value) &&
               value > 0;
    }

    private static int? ParseNullableInt(string? raw) =>
        int.TryParse(raw, out var value) ? value : null;

    private static int? TryGetNullableInt(JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var property))
        {
            return null;
        }

        if (property.ValueKind == JsonValueKind.Number && property.TryGetInt32(out var numberValue))
        {
            return numberValue;
        }

        return property.ValueKind == JsonValueKind.String && int.TryParse(property.GetString(), out var stringValue)
            ? stringValue
            : null;
    }

    private static string Required(string? value, string message)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new BadHttpRequestException(message);
        }

        return value.Trim();
    }
}
