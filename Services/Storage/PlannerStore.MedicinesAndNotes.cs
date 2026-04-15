using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    public IReadOnlyList<MedicineItem> GetMedicines() =>
        _medicines.FindAll()
            .OrderBy(x => x.Time ?? "99:99:99")
            .ThenBy(x => x.Name)
            .ToList();

    public MedicineItem UpsertMedicine(int? id, string name, string? time, int? ownerId, string? note)
    {
        MedicineItem entity;
        if (id is > 0 && _medicines.FindById(id.Value) is { } existing)
        {
            entity = existing;
        }
        else
        {
            entity = new MedicineItem
            {
                CreatedAt = DateTime.UtcNow
            };
        }

        entity.Name = name.Trim();
        entity.Time = NormalizeOptional(time);
        entity.OwnerId = ownerId;
        entity.Note = NormalizeOptional(note);

        if (entity.Id == 0)
        {
            _medicines.Insert(entity);
        }
        else
        {
            _medicines.Update(entity);
        }

        return entity;
    }

    public void ToggleMedicine(int id)
    {
        var medicine = _medicines.FindById(id);
        if (medicine is null)
        {
            return;
        }

        medicine.Taken = !medicine.Taken;
        _medicines.Update(medicine);
    }

    public void DeleteMedicine(int id) => _medicines.Delete(id);

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
