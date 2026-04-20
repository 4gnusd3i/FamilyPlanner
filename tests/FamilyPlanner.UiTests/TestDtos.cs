using System.Text.Json.Serialization;

namespace FamilyPlanner.UiTests;

public sealed class FamilyMemberDto
{
    [JsonPropertyName("id")]
    public int Id { get; init; }

    [JsonPropertyName("name")]
    public string Name { get; init; } = string.Empty;

    [JsonPropertyName("bio")]
    public string? Bio { get; init; }

    [JsonPropertyName("birthday")]
    public string? Birthday { get; init; }

    [JsonPropertyName("avatar_url")]
    public string? AvatarUrl { get; init; }
}

public sealed class PlannerEventDto
{
    [JsonPropertyName("id")]
    public int Id { get; init; }

    [JsonPropertyName("title")]
    public string Title { get; init; } = string.Empty;

    [JsonPropertyName("event_date")]
    public string EventDate { get; init; } = string.Empty;

    [JsonPropertyName("start_time")]
    public string? StartTime { get; init; }

    [JsonPropertyName("end_time")]
    public string? EndTime { get; init; }

    [JsonPropertyName("owner_id")]
    public int? OwnerId { get; init; }

    [JsonPropertyName("source_type")]
    public string? SourceType { get; init; }

    [JsonPropertyName("recurrence_type")]
    public string? RecurrenceType { get; init; }

    [JsonPropertyName("recurrence_until")]
    public string? RecurrenceUntil { get; init; }

    [JsonPropertyName("series_start_date")]
    public string? SeriesStartDate { get; init; }

    [JsonPropertyName("note")]
    public string? Note { get; init; }
}

public sealed class MealPlanDto
{
    [JsonPropertyName("id")]
    public int Id { get; init; }

    [JsonPropertyName("meal")]
    public string Meal { get; init; } = string.Empty;

    [JsonPropertyName("meal_type")]
    public string MealType { get; init; } = string.Empty;

    [JsonPropertyName("day_of_week")]
    public int DayOfWeek { get; init; }

    [JsonPropertyName("note")]
    public string? Note { get; init; }
}

public sealed class ExpenseItemDto
{
    [JsonPropertyName("id")]
    public int Id { get; init; }

    [JsonPropertyName("amount")]
    public decimal Amount { get; init; }

    [JsonPropertyName("description")]
    public string? Description { get; init; }
}

public sealed class BudgetSnapshotDto
{
    [JsonPropertyName("limit")]
    public decimal Limit { get; init; }

    [JsonPropertyName("income")]
    public decimal Income { get; init; }

    [JsonPropertyName("spent")]
    public decimal Spent { get; init; }

    [JsonPropertyName("remaining")]
    public decimal Remaining { get; init; }

    [JsonPropertyName("expenses")]
    public List<ExpenseItemDto> Expenses { get; init; } = [];
}

public sealed class NoteItemDto
{
    [JsonPropertyName("id")]
    public int Id { get; init; }

    [JsonPropertyName("title")]
    public string Title { get; init; } = string.Empty;

    [JsonPropertyName("content")]
    public string? Content { get; init; }
}

public sealed class ShoppingItemDto
{
    [JsonPropertyName("id")]
    public int Id { get; init; }

    [JsonPropertyName("item")]
    public string Item { get; init; } = string.Empty;

    [JsonPropertyName("quantity")]
    public int Quantity { get; init; }

    [JsonPropertyName("done")]
    public bool Done { get; init; }

    [JsonPropertyName("done_at")]
    public DateTime? DoneAt { get; init; }
}

public sealed class AssignmentEnvelopeDto
{
    [JsonPropertyName("assignments")]
    public List<FamilyAssignmentDto> Assignments { get; init; } = [];
}

public sealed class FamilyAssignmentDto
{
    [JsonPropertyName("day_of_week")]
    public int DayOfWeek { get; init; }

    [JsonPropertyName("family_member_id")]
    public int FamilyMemberId { get; init; }

    [JsonPropertyName("activity_type")]
    public string ActivityType { get; init; } = string.Empty;

    [JsonPropertyName("note")]
    public string? Note { get; init; }
}
