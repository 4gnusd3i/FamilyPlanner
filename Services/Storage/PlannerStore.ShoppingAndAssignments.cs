using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    private static readonly TimeSpan ShoppingDoneRetention = TimeSpan.FromSeconds(15);

    public IReadOnlyList<ShoppingItem> GetShoppingItems()
    {
        DeleteExpiredDoneShoppingItems(DateTime.Now);

        return _shoppingItems.FindAll()
            .OrderBy(x => x.Done)
            .ThenByDescending(x => x.CreatedAt)
            .ToList();
    }

    public ShoppingItem UpsertShoppingItem(int? id, string item, int quantity, int? ownerId, int? sourceMealId)
    {
        ShoppingItem entity;
        if (id is > 0 && _shoppingItems.FindById(id.Value) is { } existing)
        {
            entity = existing;
        }
        else
        {
            entity = new ShoppingItem
            {
                CreatedAt = DateTime.UtcNow
            };
        }

        entity.Item = item.Trim();
        entity.Quantity = quantity <= 0 ? 1 : quantity;
        entity.OwnerId = ownerId;
        entity.SourceMealId = sourceMealId;
        if (!entity.Done)
        {
            entity.DoneAt = null;
        }

        if (entity.Id == 0)
        {
            _shoppingItems.Insert(entity);
        }
        else
        {
            _shoppingItems.Update(entity);
        }

        return entity;
    }

    public void ToggleShoppingItem(int id)
    {
        var item = _shoppingItems.FindById(id);
        if (item is null)
        {
            return;
        }

        item.Done = !item.Done;
        item.DoneAt = item.Done ? DateTime.Now : null;
        _shoppingItems.Update(item);
    }

    public void DeleteShoppingItem(int id) => _shoppingItems.Delete(id);

    private void DeleteExpiredDoneShoppingItems(DateTime now)
    {
        foreach (var item in _shoppingItems.Find(x => x.Done).ToList())
        {
            if (item.DoneAt is null)
            {
                item.DoneAt = now;
                _shoppingItems.Update(item);
                continue;
            }

            if (now - item.DoneAt >= ShoppingDoneRetention)
            {
                _shoppingItems.Delete(item.Id);
            }
        }
    }

    public IReadOnlyList<FamilyAssignment> GetAssignments() =>
        _assignments.FindAll()
            .OrderBy(x => x.DayOfWeek)
            .ThenBy(x => x.FamilyMemberId)
            .ToList();

    public FamilyAssignment UpsertAssignment(int dayOfWeek, int familyMemberId, string activityType, string? note)
    {
        var entity = _assignments.FindOne(x => x.DayOfWeek == dayOfWeek && x.FamilyMemberId == familyMemberId)
                     ?? new FamilyAssignment
                     {
                         DayOfWeek = dayOfWeek,
                         FamilyMemberId = familyMemberId,
                         CreatedAt = DateTime.UtcNow
                     };

        entity.ActivityType = string.IsNullOrWhiteSpace(activityType) ? "medicine" : activityType.Trim();
        entity.Note = NormalizeOptional(note);

        if (entity.Id == 0)
        {
            _assignments.Insert(entity);
        }
        else
        {
            _assignments.Update(entity);
        }

        return entity;
    }

    public void RemoveAssignment(int dayOfWeek, int familyMemberId)
    {
        var existing = _assignments.FindOne(x => x.DayOfWeek == dayOfWeek && x.FamilyMemberId == familyMemberId);
        if (existing is not null)
        {
            _assignments.Delete(existing.Id);
        }
    }
}
