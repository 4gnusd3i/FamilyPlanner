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
        api.MapGet("/family/assignments", (PlannerStore store) => Results.Ok(new AssignmentEnvelope { Assignments = store.GetAssignments() }));
        api.MapPost("/family/assignments", PostAssignmentsAsync);

        api.MapGet("/medicines", (PlannerStore store) => Results.Ok(store.GetMedicines()));
        api.MapPost("/medicines", PostMedicinesAsync);

        api.MapGet("/notes", (PlannerStore store) => Results.Ok(store.GetNotes()));
        api.MapPost("/notes", PostNotesAsync);

        api.MapGet("/shopping", (PlannerStore store) => Results.Ok(store.GetShoppingItems()));
        api.MapPost("/shopping", PostShoppingAsync);
    }

    private static bool HasJsonContentType(HttpRequest request) =>
        request.ContentType?.StartsWith("application/json", StringComparison.OrdinalIgnoreCase) == true;

    private static int? ParseNullableInt(string? raw) =>
        int.TryParse(raw, out var value) ? value : null;

    private static int? TryGetNullableInt(JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var property))
        {
            return null;
        }

        return property.ValueKind is JsonValueKind.Number ? property.GetInt32() : null;
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
