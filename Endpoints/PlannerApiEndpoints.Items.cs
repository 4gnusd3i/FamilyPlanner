using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Localization;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static async Task<IResult> PostNotesAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions, AppLocalizationService localization)
    {
        if (HasJsonContentType(request))
        {
            var command = await request.ReadFromJsonAsync<DeleteRequest>(jsonOptions.Value.SerializerOptions);
            if (command?.Delete == true)
            {
                if (command.Id <= 0)
                {
                    return BadRequest(request.HttpContext, localization, "errors.notes.invalid_note");
                }

                store.DeleteNote(command.Id);
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var title = Required(form["title"]);
        if (title is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.notes.title_required");
        }

        store.UpsertNote(ParseNullableInt(form["id"]), title, ParseNullableInt(form["owner_id"]), form["content"]);
        return Results.Ok(new { ok = true });
    }

    private static async Task<IResult> PostShoppingAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions, AppLocalizationService localization)
    {
        if (HasJsonContentType(request))
        {
            var body = await ReadJsonObjectAsync(request, jsonOptions.Value);
            if (body is null)
            {
                return BadRequest(request.HttpContext, localization, "errors.shopping.invalid_request");
            }

            if (HasTrueProperty(body.Value, "toggle"))
            {
                if (!TryGetRequiredInt(body.Value, "id", out var id))
                {
                    return BadRequest(request.HttpContext, localization, "errors.shopping.invalid_item");
                }

                store.ToggleShoppingItem(id);
                return Results.Ok(new { ok = true });
            }

            if (HasTrueProperty(body.Value, "delete"))
            {
                if (!TryGetRequiredInt(body.Value, "id", out var id))
                {
                    return BadRequest(request.HttpContext, localization, "errors.shopping.invalid_item");
                }

                store.DeleteShoppingItem(id);
                return Results.Ok(new { ok = true });
            }

            var item = body.Value.TryGetProperty("item", out var itemElement) ? itemElement.GetString() : null;
            var quantity = body.Value.TryGetProperty("quantity", out var quantityElement) ? quantityElement.GetInt32() : 1;
            var ownerId = TryGetNullableInt(body.Value, "owner_id");
            var itemName = Required(item);
            if (itemName is null)
            {
                return BadRequest(request.HttpContext, localization, "errors.shopping.item_required");
            }

            store.UpsertShoppingItem(null, itemName, quantity, ownerId);
            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        var itemValue = Required(form["item"]);
        if (itemValue is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.shopping.item_required");
        }

        var quantityValue = int.TryParse(form["quantity"], out var parsedQuantity) ? parsedQuantity : 1;
        store.UpsertShoppingItem(
            ParseNullableInt(form["id"]),
            itemValue,
            quantityValue,
            ParseNullableInt(form["owner_id"]));

        return Results.Ok(new { ok = true });
    }
}
