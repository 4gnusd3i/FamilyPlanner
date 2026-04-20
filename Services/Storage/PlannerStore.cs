using LiteDB;
using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore : IDisposable
{
    private static readonly string[] ObsoleteCollections =
    [
        "users",
        "medicines"
    ];

    private readonly LiteDatabase _database;
    private readonly ILiteCollection<HouseholdProfile> _householdProfiles;
    private readonly ILiteCollection<FamilyMember> _familyMembers;
    private readonly ILiteCollection<PlannerEvent> _events;
    private readonly ILiteCollection<MealPlan> _meals;
    private readonly ILiteCollection<BudgetMonth> _budgetMonths;
    private readonly ILiteCollection<ExpenseItem> _expenses;
    private readonly ILiteCollection<NoteItem> _notes;
    private readonly ILiteCollection<ShoppingItem> _shoppingItems;
    private readonly ILiteCollection<FamilyAssignment> _assignments;

    public PlannerStore(StoragePaths storagePaths)
    {
        _database = new LiteDatabase(storagePaths.DatabasePath);
        _householdProfiles = _database.GetCollection<HouseholdProfile>("householdProfile");
        _familyMembers = _database.GetCollection<FamilyMember>("familyMembers");
        _events = _database.GetCollection<PlannerEvent>("events");
        _meals = _database.GetCollection<MealPlan>("meals");
        _budgetMonths = _database.GetCollection<BudgetMonth>("budgetMonths");
        _expenses = _database.GetCollection<ExpenseItem>("expenses");
        _notes = _database.GetCollection<NoteItem>("notes");
        _shoppingItems = _database.GetCollection<ShoppingItem>("shoppingItems");
        _assignments = _database.GetCollection<FamilyAssignment>("familyAssignments");

        EnsureIndexes();
    }

    public bool HasHouseholdProfile() => _householdProfiles.FindById(1) is not null;

    public HouseholdProfile? GetHouseholdProfile() => _householdProfiles.FindById(1);

    public HouseholdProfile InitializeHousehold(string familyName)
    {
        if (HasHouseholdProfile())
        {
            throw new InvalidOperationException("Household profile already exists.");
        }

        var profile = new HouseholdProfile
        {
            Id = 1,
            FamilyName = familyName.Trim(),
            CreatedAt = DateTime.UtcNow
        };

        _householdProfiles.Upsert(profile);
        return profile;
    }

    public void ResetForFreshSetup()
    {
        _householdProfiles.DeleteAll();
        _familyMembers.DeleteAll();
        _events.DeleteAll();
        _meals.DeleteAll();
        _budgetMonths.DeleteAll();
        _expenses.DeleteAll();
        _notes.DeleteAll();
        _shoppingItems.DeleteAll();
        _assignments.DeleteAll();
        DropObsoleteCollections();
    }

    public void RunMaintenance()
    {
        DropObsoleteCollections();
        EnsureIndexes();
        DeleteExpiredDoneShoppingItems(DateTime.Now);
    }

    public void Dispose() => _database.Dispose();

    private static string? NormalizeOptional(string? value) =>
        string.IsNullOrWhiteSpace(value) ? null : value.Trim();

    private static string CurrentMonth() => DateOnly.FromDateTime(DateTime.Now).ToString("yyyy-MM");

    private void EnsureIndexes()
    {
        _familyMembers.EnsureIndex(x => x.CreatedAt);
        _events.EnsureIndex(x => x.EventDate);
        _events.EnsureIndex(x => x.SourceType);
        _events.EnsureIndex(x => x.SourceMemberId);
        _events.EnsureIndex(x => x.SourceYear);
        _events.EnsureIndex(x => x.RecurrenceType);
        _events.EnsureIndex(x => x.RecurrenceUntil);
        _budgetMonths.EnsureIndex(x => x.Month);
        _expenses.EnsureIndex(x => x.Month);
        _notes.EnsureIndex(x => x.CreatedAt);
        _shoppingItems.EnsureIndex(x => x.Done);
        _shoppingItems.EnsureIndex(x => x.DoneAt);
        _assignments.EnsureIndex(x => x.DayOfWeek);
        _assignments.EnsureIndex(x => x.FamilyMemberId);
    }

    private void DropObsoleteCollections()
    {
        foreach (var collectionName in ObsoleteCollections)
        {
            _database.DropCollection(collectionName);
        }
    }
}
