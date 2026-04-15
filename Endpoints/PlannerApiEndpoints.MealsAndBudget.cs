using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static async Task<IResult> PostMealAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions)
    {
        if (HasJsonContentType(request))
        {
            var command = await request.ReadFromJsonAsync<DeleteRequest>(jsonOptions.Value.SerializerOptions);
            if (command?.Delete == true)
            {
                store.DeleteMeal(command.Id);
            }

            return Results.Ok(new { ok = true });
        }

        var form = await request.ReadFormAsync();
        if (!int.TryParse(form["day_of_week"], out var dayOfWeek))
        {
            return Results.BadRequest(new { error = "Ugyldig ukedag." });
        }

        var meal = Required(form["meal"], "Måltid mangler.");
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

    private static async Task<IResult> PostBudgetAsync(HttpRequest request, PlannerStore store, IOptions<JsonOptions> jsonOptions)
    {
        var body = await request.ReadFromJsonAsync<JsonElement>(jsonOptions.Value.SerializerOptions);

        if (body.TryGetProperty("delete_expense", out var deleteExpenseElement) && deleteExpenseElement.GetBoolean())
        {
            store.DeleteExpense(body.GetProperty("id").GetInt32());
            return Results.Ok(new { ok = true });
        }

        if (body.TryGetProperty("set_budget", out var setBudgetElement) && setBudgetElement.GetBoolean())
        {
            var month = body.TryGetProperty("month", out var monthElement) && !string.IsNullOrWhiteSpace(monthElement.GetString())
                ? monthElement.GetString()!
                : DateOnly.FromDateTime(DateTime.Now).ToString("yyyy-MM");
            var limit = body.TryGetProperty("limit", out var limitElement) ? limitElement.GetDecimal() : 0;
            var income = body.TryGetProperty("income", out var incomeElement) ? incomeElement.GetDecimal() : 0;
            store.SetBudget(month, limit, income);
            return Results.Ok(new { ok = true });
        }

        var amount = body.TryGetProperty("amount", out var amountElement) ? amountElement.GetDecimal() : 0;
        var category = body.TryGetProperty("category", out var categoryElement) ? categoryElement.GetString() : null;
        var expenseDate = body.TryGetProperty("expense_date", out var dateElement) ? dateElement.GetString() : null;
        var ownerId = TryGetNullableInt(body, "owner_id");
        var description = body.TryGetProperty("description", out var descriptionElement) ? descriptionElement.GetString() : null;

        store.AddExpense(amount, category, expenseDate, ownerId, description);
        return Results.Ok(new { ok = true });
    }

}
