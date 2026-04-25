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
            var command = await ReadJsonAsync<DeleteRequest>(request, jsonOptions.Value);
            if (command is null)
            {
                return BadRequest(request.HttpContext, localization, "errors.notes.invalid_note");
            }

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

            if (!TryGetOptionalString(body.Value, "item", out var item))
            {
                return BadRequest(request.HttpContext, localization, "errors.shopping.item_required");
            }

            if (!TryGetOptionalPositiveInt(body.Value, "quantity", 1, out var quantity))
            {
                return BadRequest(request.HttpContext, localization, "errors.shopping.invalid_quantity");
            }

            if (!TryGetOptionalNullableInt(body.Value, "owner_id", out var ownerId))
            {
                return BadRequest(request.HttpContext, localization, "errors.shopping.invalid_item");
            }

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

        var quantityValue = string.IsNullOrWhiteSpace(form["quantity"])
            ? 1
            : int.TryParse(form["quantity"], out var parsedQuantity) && parsedQuantity > 0
                ? parsedQuantity
                : 0;
        if (quantityValue <= 0)
        {
            return BadRequest(request.HttpContext, localization, "errors.shopping.invalid_quantity");
        }

        store.UpsertShoppingItem(
            ParseNullableInt(form["id"]),
            itemValue,
            quantityValue,
            ParseNullableInt(form["owner_id"]));

        return Results.Ok(new { ok = true });
    }
}
