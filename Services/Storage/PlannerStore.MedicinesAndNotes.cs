using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    public IReadOnlyList<NoteItem> GetNotes() =>
        _notes.FindAll()
            .OrderByDescending(x => x.CreatedAt)
            .ToList();

    public NoteItem UpsertNote(int? id, string title, int? ownerId, string? content)
    {
        NoteItem entity;
        if (id is > 0 && _notes.FindById(id.Value) is { } existing)
        {
            entity = existing;
        }
        else
        {
            entity = new NoteItem
            {
                CreatedAt = DateTime.UtcNow
            };
        }

        entity.Title = title.Trim();
        entity.OwnerId = ownerId;
        entity.Content = NormalizeOptional(content);

        if (entity.Id == 0)
        {
            _notes.Insert(entity);
        }
        else
        {
            _notes.Update(entity);
        }

        return entity;
    }

    public void DeleteNote(int id) => _notes.Delete(id);
}
