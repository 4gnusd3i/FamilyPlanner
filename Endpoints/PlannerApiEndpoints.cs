using System.Globalization;
using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Localization;
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
        try
        {
            var body = await request.ReadFromJsonAsync<JsonElement>(jsonOptions.SerializerOptions);
            return body.ValueKind == JsonValueKind.Object ? body : null;
        }
        catch (JsonException)
        {
            return null;
        }
    }

    private static async Task<T?> ReadJsonAsync<T>(HttpRequest request, JsonOptions jsonOptions)
    {
        try
        {
            return await request.ReadFromJsonAsync<T>(jsonOptions.SerializerOptions);
        }
        catch (JsonException)
        {
            return default;
        }
    }

    private static IResult BadRequest(HttpContext context, AppLocalizationService localization, string key, IReadOnlyDictionary<string, string?>? placeholders = null) =>
        Results.BadRequest(new { error = localization.Format(context, key, placeholders) });

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

    private static bool TryGetOptionalString(JsonElement element, string propertyName, out string? value)
    {
        value = null;
        if (!element.TryGetProperty(propertyName, out var property) || property.ValueKind == JsonValueKind.Null)
        {
            return true;
        }

        if (property.ValueKind == JsonValueKind.String)
        {
            value = property.GetString();
            return true;
        }

        return false;
    }

    private static bool TryGetOptionalNullableInt(JsonElement element, string propertyName, out int? value)
    {
        value = null;
        if (!element.TryGetProperty(propertyName, out var property) || property.ValueKind == JsonValueKind.Null)
        {
            return true;
        }

        if (property.ValueKind == JsonValueKind.String && string.IsNullOrWhiteSpace(property.GetString()))
        {
            return true;
        }

        if (property.ValueKind == JsonValueKind.Number && property.TryGetInt32(out var numberValue) && numberValue > 0)
        {
            value = numberValue;
            return true;
        }

        if (property.ValueKind == JsonValueKind.String &&
            int.TryParse(property.GetString(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var stringValue) &&
            stringValue > 0)
        {
            value = stringValue;
            return true;
        }

        return false;
    }

    private static bool TryGetOptionalDecimal(JsonElement element, string propertyName, out decimal value)
    {
        value = 0;
        if (!element.TryGetProperty(propertyName, out var property) || property.ValueKind == JsonValueKind.Null)
        {
            return true;
        }

        if (property.ValueKind == JsonValueKind.Number)
        {
            return property.TryGetDecimal(out value);
        }

        if (property.ValueKind == JsonValueKind.String)
        {
            return decimal.TryParse(property.GetString(), NumberStyles.Number, CultureInfo.InvariantCulture, out value);
        }

        return false;
    }

    private static bool TryGetRequiredPositiveDecimal(JsonElement element, string propertyName, out decimal value) =>
        TryGetOptionalDecimal(element, propertyName, out value) &&
        element.TryGetProperty(propertyName, out var property) &&
        property.ValueKind != JsonValueKind.Null &&
        value > 0;

    private static bool TryGetOptionalPositiveInt(JsonElement element, string propertyName, int defaultValue, out int value)
    {
        value = defaultValue;
        if (!element.TryGetProperty(propertyName, out var property) || property.ValueKind == JsonValueKind.Null)
        {
            return true;
        }

        if (property.ValueKind == JsonValueKind.Number && property.TryGetInt32(out var numberValue))
        {
            value = numberValue;
            return value > 0;
        }

        if (property.ValueKind == JsonValueKind.String &&
            int.TryParse(property.GetString(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var stringValue))
        {
            value = stringValue;
            return value > 0;
        }

        return false;
    }

    private static bool IsValidIsoDate(string? value) =>
        string.IsNullOrWhiteSpace(value) ||
        DateOnly.TryParseExact(value, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out _);

    private static bool IsValidIsoMonth(string value) =>
        DateOnly.TryParseExact($"{value}-01", "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out _);

    private static bool IsValidTime(string? value) =>
        string.IsNullOrWhiteSpace(value) ||
        TimeOnly.TryParseExact(value.Trim(), "HH:mm", CultureInfo.InvariantCulture, DateTimeStyles.None, out _) ||
        TimeOnly.TryParseExact(value.Trim(), "HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out _);

    private static string? Required(string? value) =>
        string.IsNullOrWhiteSpace(value) ? null : value.Trim();
}
