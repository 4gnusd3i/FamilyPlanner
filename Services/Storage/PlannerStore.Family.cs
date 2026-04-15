using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    public IReadOnlyList<FamilyMember> GetFamilyMembers() =>
        _familyMembers.FindAll().OrderBy(x => x.CreatedAt).ToList();

    public FamilyMember? GetFamilyMemberById(int id) => _familyMembers.FindById(id);

    public FamilyMember UpsertFamilyMember(
        int? id,
        string name,
        string? birthday,
        string? bio,
        string? color,
        string? avatarUrl)
    {
        FamilyMember member;

        if (id is > 0 && _familyMembers.FindById(id.Value) is { } existing)
        {
            member = existing;
        }
        else
        {
            member = new FamilyMember
            {
                CreatedAt = DateTime.UtcNow
            };
        }

        member.Name = name.Trim();
        member.Birthday = NormalizeOptional(birthday);
        member.Bio = NormalizeOptional(bio);
        member.Color = string.IsNullOrWhiteSpace(color) ? "#3b82f6" : color.Trim();
        member.AvatarUrl = NormalizeOptional(avatarUrl);

        if (member.Id == 0)
        {
            _familyMembers.Insert(member);
        }
        else
        {
            _familyMembers.Update(member);
        }

        DeleteBirthdayEventsForMember(member.Id);
        return member;
    }

    public void DeleteFamilyMember(int id)
    {
        DeleteBirthdayEventsForMember(id);

        foreach (var item in _events.Find(x => x.OwnerId == id))
        {
            item.OwnerId = null;
            _events.Update(item);
        }

        foreach (var item in _meals.Find(x => x.OwnerId == id))
        {
            item.OwnerId = null;
            _meals.Update(item);
        }

        foreach (var item in _expenses.Find(x => x.OwnerId == id))
        {
            item.OwnerId = null;
            _expenses.Update(item);
        }

        foreach (var item in _medicines.Find(x => x.OwnerId == id))
        {
            item.OwnerId = null;
            _medicines.Update(item);
        }

        foreach (var item in _notes.Find(x => x.OwnerId == id))
        {
            item.OwnerId = null;
            _notes.Update(item);
        }

        foreach (var item in _shoppingItems.Find(x => x.OwnerId == id))
        {
            item.OwnerId = null;
            _shoppingItems.Update(item);
        }

        foreach (var assignment in _assignments.Find(x => x.FamilyMemberId == id).ToList())
        {
            _assignments.Delete(assignment.Id);
        }

        _familyMembers.Delete(id);
    }

}
