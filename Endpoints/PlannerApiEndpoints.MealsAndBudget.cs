using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Localization;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static async Task<IResult> PostMealAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions, AppLocalizationService localization)
    {
        if (HasJsonContentType(request))
        {
            var command = await request.ReadFromJsonAsync<DeleteRequest>(jsonOptions.Value.SerializerOptions);
            if (command?.Delete == true)
            {
                if (command.Id <= 0)
                {
                    return BadRequest(request.HttpContext, localization, "errors.meals.invalid_meal");
                }

                store.DeleteMeal(command.Id);
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        if (!int.TryParse(form["day_of_week"], out var dayOfWeek))
        {
            return BadRequest(request.HttpContext, localization, "errors.meals.invalid_day_of_week");
        }

        var meal = Required(form["meal"]);
        if (meal is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.meals.meal_required");
        }

        var mealType = string.IsNullOrWhiteSpace(form["meal_type"]) ? "dinner" : form["meal_type"].ToString();
        store.UpsertMeal(
            ParseNullableInt(form["id"]),
            dayOfWeek,
            mealType,
            meal,
            ParseNullableInt(form["owner_id"]),
            form["note"]);

        return Results.Ok(new { ok = true });
    }

    private static async Task<IResult> PostBudgetAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions, AppLocalizationService localization)
    {
        var body = await ReadJsonObjectAsync(request, jsonOptions.Value);
        if (body is null)
        {
            return BadRequest(request.HttpContext, localization, "errors.budget.invalid_request");
        }

        if (HasTrueProperty(body.Value, "delete_expense"))
        {
            if (!TryGetRequiredInt(body.Value, "id", out var id))
            {
                return BadRequest(request.HttpContext, localization, "errors.budget.invalid_expense");
            }

            store.DeleteExpense(id);
            return Results.Ok(new { ok = true });
        }

        if (HasTrueProperty(body.Value, "set_budget"))
        {
            var month = body.Value.TryGetProperty("month", out var monthElement) && !string.IsNullOrWhiteSpace(monthElement.GetString())
                ? monthElement.GetString()!
                : DateOnly.FromDateTime(DateTime.Now).ToString("yyyy-MM");
            var limit = body.Value.TryGetProperty("limit", out var limitElement) ? limitElement.GetDecimal() : 0;
            var income = body.Value.TryGetProperty("income", out var incomeElement) ? incomeElement.GetDecimal() : 0;
            store.SetBudget(month, limit, income);
            return Results.Ok(new { ok = true });
        }

        var amount = body.Value.TryGetProperty("amount", out var amountElement) ? amountElement.GetDecimal() : 0;
        var category = body.Value.TryGetProperty("category", out var categoryElement) ? categoryElement.GetString() : null;
        var expenseDate = body.Value.TryGetProperty("expense_date", out var dateElement) ? dateElement.GetString() : null;
        var ownerId = TryGetNullableInt(body.Value, "owner_id");
        var description = body.Value.TryGetProperty("description", out var descriptionElement) ? descriptionElement.GetString() : null;

        store.AddExpense(amount, category, expenseDate, ownerId, description);
        return Results.Ok(new { ok = true });
    }

}
