using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    public IReadOnlyList<PlannerEvent> GetEvents(DateOnly start, DateOnly end)
    {
        var startText = start.ToString("yyyy-MM-dd");
        var endText = end.ToString("yyyy-MM-dd");

        return _events.FindAll()
            .Where(x => !string.IsNullOrWhiteSpace(x.EventDate) &&
                        string.CompareOrdinal(x.EventDate, startText) >= 0 &&
                        string.CompareOrdinal(x.EventDate, endText) <= 0)
            .OrderBy(x => x.EventDate)
            .ThenBy(x => x.StartTime ?? "99:99:99")
            .ThenBy(x => x.Title)
            .ToList();
    }

    public IReadOnlyList<PlannerEvent> GetUpcomingEvents(DateOnly fromDate)
    {
        var fromText = fromDate.ToString("yyyy-MM-dd");
        return _events.FindAll()
            .Where(x => !string.IsNullOrWhiteSpace(x.EventDate) &&
                        string.CompareOrdinal(x.EventDate, fromText) >= 0)
            .OrderBy(x => x.EventDate)
            .ThenBy(x => x.StartTime ?? "99:99:99")
            .ThenBy(x => x.Title)
            .ToList();
    }

    public PlannerEvent UpsertEvent(
        int? id,
        string title,
        string eventDate,
        string? startTime,
        string? endTime,
        int? ownerId,
        string? color,
        string? note)
    {
        PlannerEvent entity;
        if (id is > 0 && _events.FindById(id.Value) is { } existing)
        {
            entity = existing;
        }
        else
        {
            entity = new PlannerEvent
            {
                CreatedAt = DateTime.UtcNow
            };
        }

        entity.Title = title.Trim();
        entity.EventDate = eventDate;
        entity.StartTime = NormalizeOptional(startTime);
        entity.EndTime = NormalizeOptional(endTime);
        entity.OwnerId = ownerId;
        entity.Color = NormalizeOptional(color) ?? "#3b82f6";
        entity.Note = NormalizeOptional(note);

        if (entity.Id == 0)
        {
            _events.Insert(entity);
        }
        else
        {
            _events.Update(entity);
        }

        return entity;
    }

    public void DeleteEvent(int id) => _events.Delete(id);

}
