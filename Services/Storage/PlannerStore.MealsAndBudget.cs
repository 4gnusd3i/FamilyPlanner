using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    public IReadOnlyList<MealPlan> GetMeals() =>
        _meals.FindAll()
            .OrderBy(x => x.DayOfWeek)
            .ThenBy(x => x.MealType)
            .ThenBy(x => x.CreatedAt)
            .ToList();

    public MealPlan UpsertMeal(int? id, int dayOfWeek, string mealType, string meal, int? ownerId, string? note)
    {
        MealPlan entity;
        if (id is > 0 && _meals.FindById(id.Value) is { } existing)
        {
            entity = existing;
        }
        else
        {
            entity = new MealPlan
            {
                CreatedAt = DateTime.UtcNow
            };
        }

        entity.DayOfWeek = dayOfWeek;
        entity.MealType = mealType;
        entity.Meal = meal.Trim();
        entity.OwnerId = ownerId;
        entity.Note = NormalizeOptional(note);

        if (entity.Id == 0)
        {
            _meals.Insert(entity);
        }
        else
        {
            _meals.Update(entity);
        }

        return entity;
    }

    public void DeleteMeal(int id) => _meals.Delete(id);

    public BudgetSnapshot GetBudgetSnapshot(string? month = null)
    {
        var effectiveMonth = string.IsNullOrWhiteSpace(month) ? CurrentMonth() : month;
        var budgetMonth = _budgetMonths.FindOne(x => x.Month == effectiveMonth);
        var expenses = _expenses.Find(x => x.Month == effectiveMonth)
            .OrderByDescending(x => x.ExpenseDate ?? string.Empty)
            .ThenByDescending(x => x.CreatedAt)
            .ToList();
        var spent = expenses.Sum(x => x.Amount);

        return new BudgetSnapshot
        {
            Limit = budgetMonth?.Limit ?? 0,
            Income = budgetMonth?.Income ?? 0,
            Spent = spent,
            Remaining = (budgetMonth?.Limit ?? 0) - spent,
            Available = (budgetMonth?.Income ?? 0) - spent,
            Expenses = expenses
        };
    }

    public BudgetMonth SetBudget(string month, decimal limit, decimal income)
    {
        var entity = _budgetMonths.FindOne(x => x.Month == month) ?? new BudgetMonth
        {
            Month = month,
            CreatedAt = DateTime.UtcNow
        };

        entity.Limit = limit;
        entity.Income = income;
        entity.UpdatedAt = DateTime.UtcNow;

        if (entity.Id == 0)
        {
            _budgetMonths.Insert(entity);
        }
        else
        {
            _budgetMonths.Update(entity);
        }

        return entity;
    }

    public ExpenseItem AddExpense(decimal amount, string? category, string? expenseDate, int? ownerId, string? description)
    {
        var effectiveDate = NormalizeOptional(expenseDate) ?? DateOnly.FromDateTime(DateTime.Now).ToString("yyyy-MM-dd");
        var entity = new ExpenseItem
        {
            Amount = amount,
            Category = string.IsNullOrWhiteSpace(category) ? "Annet" : category.Trim(),
            ExpenseDate = effectiveDate,
            OwnerId = ownerId,
            Description = NormalizeOptional(description),
            Month = effectiveDate[..7],
            CreatedAt = DateTime.UtcNow
        };

        _expenses.Insert(entity);
        return entity;
    }

    public void DeleteExpense(int id) => _expenses.Delete(id);

}
