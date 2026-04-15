using LiteDB;

namespace FamilyPlanner.Models;

public abstract class EntityBase
{
    [BsonId]
    public int Id { get; set; }

    public int UserId { get; set; } = 1;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public sealed class HouseholdProfile
{
    [BsonId]
    public int Id { get; set; } = 1;
    public string FamilyName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public sealed class FamilyMember : EntityBase
{
    public string Name { get; set; } = string.Empty;
    public string Color { get; set; } = "#3b82f6";
    public string? AvatarUrl { get; set; }
    public string? Birthday { get; set; }
    public string? Bio { get; set; }
}

public sealed class PlannerEvent : EntityBase
{
    public string Title { get; set; } = string.Empty;
    public string EventDate { get; set; } = string.Empty;
    public string? StartTime { get; set; }
    public string? EndTime { get; set; }
    public int? OwnerId { get; set; }
    public string? Color { get; set; }
    public string? Note { get; set; }
}

public sealed class MealPlan : EntityBase
{
    public int DayOfWeek { get; set; }
    public string MealType { get; set; } = "dinner";
    public string Meal { get; set; } = string.Empty;
    public int? OwnerId { get; set; }
    public string? Note { get; set; }
}

public sealed class BudgetMonth : EntityBase
{
    public string Month { get; set; } = string.Empty;
    public decimal Limit { get; set; }
    public decimal Income { get; set; }
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public sealed class ExpenseItem : EntityBase
{
    public decimal Amount { get; set; }
    public string Category { get; set; } = "Annet";
    public string? ExpenseDate { get; set; }
    public int? OwnerId { get; set; }
    public string? Description { get; set; }
    public string Month { get; set; } = string.Empty;
}

public sealed class MedicineItem : EntityBase
{
    public string Name { get; set; } = string.Empty;
    public string? Time { get; set; }
    public int? OwnerId { get; set; }
    public string? Note { get; set; }
    public bool Taken { get; set; }
}

public sealed class NoteItem : EntityBase
{
    public string Title { get; set; } = string.Empty;
    public int? OwnerId { get; set; }
    public string? Content { get; set; }
}

public sealed class ShoppingItem : EntityBase
{
    public string Item { get; set; } = string.Empty;
    public int? OwnerId { get; set; }
    public int Quantity { get; set; } = 1;
    public bool Done { get; set; }
    public int? SourceMealId { get; set; }
}

public sealed class FamilyAssignment : EntityBase
{
    public int DayOfWeek { get; set; }
    public int FamilyMemberId { get; set; }
    public string ActivityType { get; set; } = "medicine";
    public string? Note { get; set; }
}

public sealed class BudgetSnapshot
{
    public decimal Limit { get; set; }
    public decimal Income { get; set; }
    public decimal Spent { get; set; }
    public decimal Remaining { get; set; }
    public decimal Available { get; set; }
    public IReadOnlyList<ExpenseItem> Expenses { get; set; } = [];
}

public sealed class AssignmentEnvelope
{
    public IReadOnlyList<FamilyAssignment> Assignments { get; set; } = [];
}
