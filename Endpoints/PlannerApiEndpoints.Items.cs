using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static async Task<IResult> PostMedicinesAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions)
    {
        if (HasJsonContentType(request))
        {
            var body = await request.ReadFromJsonAsync<JsonElement>(jsonOptions.Value.SerializerOptions);
            if (body.TryGetProperty("toggle", out var toggleElement) && toggleElement.GetBoolean())
            {
                store.ToggleMedicine(body.GetProperty("id").GetInt32());
            }
            else if (body.TryGetProperty("delete", out var deleteElement) && deleteElement.GetBoolean())
            {
                store.DeleteMedicine(body.GetProperty("id").GetInt32());
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var name = Required(form["name"], "Navn mangler.");
        store.UpsertMedicine(
            ParseNullableInt(form["id"]),
            name,
            form["time"],
            ParseNullableInt(form["owner_id"]),
            form["note"]);

        return Results.Ok(new { ok = true });
    }

    private static async Task<IResult> PostNotesAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions)
    {
        if (HasJsonContentType(request))
        {
            var command = await request.ReadFromJsonAsync<DeleteRequest>(jsonOptions.Value.SerializerOptions);
            if (command?.Delete == true)
            {
                store.DeleteNote(command.Id);
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var title = Required(form["title"], "Tittel mangler.");
        store.UpsertNote(ParseNullableInt(form["id"]), title, ParseNullableInt(form["owner_id"]), form["content"]);
        return Results.Ok(new { ok = true });
    }

    private static async Task<IResult> PostShoppingAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions)
    {
        if (HasJsonContentType(request))
        {
            var body = await request.ReadFromJsonAsync<JsonElement>(jsonOptions.Value.SerializerOptions);
            if (body.TryGetProperty("toggle", out var toggleElement) && toggleElement.GetBoolean())
            {
                store.ToggleShoppingItem(body.GetProperty("id").GetInt32());
                return Results.Ok(new { ok = true });
            }

            if (body.TryGetProperty("delete", out var deleteElement) && deleteElement.GetBoolean())
            {
                store.DeleteShoppingItem(body.GetProperty("id").GetInt32());
                return Results.Ok(new { ok = true });
            }

            var item = body.TryGetProperty("item", out var itemElement) ? itemElement.GetString() : null;
            var quantity = body.TryGetProperty("quantity", out var quantityElement) ? quantityElement.GetInt32() : 1;
            var sourceMealId = TryGetNullableInt(body, "source_meal_id");
            var ownerId = TryGetNullableInt(body, "owner_id");

            store.UpsertShoppingItem(null, Required(item, "Vare mangler."), quantity, ownerId, sourceMealId);
            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var itemValue = Required(form["item"], "Vare mangler.");
        var quantityValue = int.TryParse(form["quantity"], out var parsedQuantity) ? parsedQuantity : 1;
        store.UpsertShoppingItem(
            ParseNullableInt(form["id"]),
            itemValue,
            quantityValue,
            ParseNullableInt(form["owner_id"]),
            ParseNullableInt(form["source_meal_id"]));

        return Results.Ok(new { ok = true });
    }

}
