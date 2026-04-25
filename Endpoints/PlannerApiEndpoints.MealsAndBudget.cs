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
            var command = await ReadJsonAsync<DeleteRequest>(request, jsonOptions.Value);
            if (command is null)
            {
                return BadRequest(request.HttpContext, localization, "errors.meals.invalid_meal");
            }

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
        if (!int.TryParse(form["day_of_week"], out var dayOfWeek) || dayOfWeek is < 0 or > 6)
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
            if (!TryGetOptionalString(body.Value, "month", out var requestedMonth))
            {
                return BadRequest(request.HttpContext, localization, "errors.budget.invalid_month");
            }

            var month = string.IsNullOrWhiteSpace(requestedMonth)
                ? DateOnly.FromDateTime(DateTime.Now).ToString("yyyy-MM")
                : requestedMonth.Trim();
            if (!IsValidIsoMonth(month))
            {
                return BadRequest(request.HttpContext, localization, "errors.budget.invalid_month");
            }

            if (!TryGetOptionalDecimal(body.Value, "limit", out var limit) ||
                !TryGetOptionalDecimal(body.Value, "income", out var income) ||
                limit < 0 ||
                income < 0)
            {
                return BadRequest(request.HttpContext, localization, "errors.budget.invalid_request");
            }

            store.SetBudget(month, limit, income);
            return Results.Ok(new { ok = true });
        }

        if (!TryGetRequiredPositiveDecimal(body.Value, "amount", out var amount))
        {
            return BadRequest(request.HttpContext, localization, "errors.budget.invalid_amount");
        }

        if (!TryGetOptionalString(body.Value, "category", out var category) ||
            !TryGetOptionalString(body.Value, "description", out var description))
        {
            return BadRequest(request.HttpContext, localization, "errors.budget.invalid_request");
        }

        if (!TryGetOptionalString(body.Value, "expense_date", out var expenseDate) || !IsValidIsoDate(expenseDate))
        {
            return BadRequest(request.HttpContext, localization, "errors.budget.invalid_date");
        }

        if (!TryGetOptionalNullableInt(body.Value, "owner_id", out var ownerId))
        {
            return BadRequest(request.HttpContext, localization, "errors.budget.invalid_request");
        }

        store.AddExpense(amount, category, expenseDate, ownerId, description);
        return Results.Ok(new { ok = true });
    }

}
