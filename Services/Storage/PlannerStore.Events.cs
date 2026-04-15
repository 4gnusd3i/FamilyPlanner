using System.Globalization;
using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    private const string BirthdaySourceType = "birthday";

    public IReadOnlyList<PlannerEvent> GetEvents(DateOnly start, DateOnly end)
    {
        EnsureBirthdayEvents(start, end);

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
        var now = DateTime.Now;
        EnsureBirthdayEvents(fromDate, fromDate.AddYears(1));

        return _events.FindAll()
            .Where(x => IsUpcomingEvent(x, now))
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

    private void EnsureBirthdayEvents(DateOnly start, DateOnly end)
    {
        if (end < start)
        {
            return;
        }

        var familyMembers = _familyMembers.FindAll()
            .Where(x => !string.IsNullOrWhiteSpace(x.Birthday))
            .ToList();

        foreach (var member in familyMembers)
        {
            for (var year = start.Year; year <= end.Year; year += 1)
            {
                if (!TryGetBirthdayDate(member.Birthday, year, out var birthdayDate) ||
                    birthdayDate < start ||
                    birthdayDate > end)
                {
                    continue;
                }

                UpsertBirthdayEvent(member, birthdayDate, year);
            }
        }
    }

    private void UpsertBirthdayEvent(FamilyMember member, DateOnly birthdayDate, int year)
    {
        var title = $"{member.Name} har bursdag";
        var eventDate = birthdayDate.ToString("yyyy-MM-dd");
        var color = string.IsNullOrWhiteSpace(member.Color) ? "#ffafcc" : member.Color;
        var existing = _events.FindOne(x =>
            x.SourceType == BirthdaySourceType &&
            x.SourceMemberId == member.Id &&
            x.SourceYear == year);

        if (existing is null)
        {
            _events.Insert(new PlannerEvent
            {
                Title = title,
                EventDate = eventDate,
                OwnerId = member.Id,
                Color = color,
                Note = "Fødselsdag",
                SourceType = BirthdaySourceType,
                SourceMemberId = member.Id,
                SourceYear = year,
                CreatedAt = DateTime.UtcNow
            });
            return;
        }

        existing.Title = title;
        existing.EventDate = eventDate;
        existing.OwnerId = member.Id;
        existing.Color = color;
        existing.Note = "Fødselsdag";
        _events.Update(existing);
    }

    private static bool TryGetBirthdayDate(string? birthday, int year, out DateOnly birthdayDate)
    {
        birthdayDate = default;
        if (!DateOnly.TryParseExact(birthday, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var parsedBirthday))
        {
            return false;
        }

        if (parsedBirthday.Month == 2 && parsedBirthday.Day == 29 && !DateTime.IsLeapYear(year))
        {
            birthdayDate = new DateOnly(year, 2, 28);
            return true;
        }

        birthdayDate = new DateOnly(year, parsedBirthday.Month, parsedBirthday.Day);
        return true;
    }

    private static bool IsUpcomingEvent(PlannerEvent plannerEvent, DateTime now)
    {
        if (!DateOnly.TryParse(plannerEvent.EventDate, out var eventDate))
        {
            return false;
        }

        var today = DateOnly.FromDateTime(now);
        if (eventDate > today)
        {
            return true;
        }

        if (eventDate < today)
        {
            return false;
        }

        var cutoffTime = NormalizeOptional(plannerEvent.EndTime) ?? NormalizeOptional(plannerEvent.StartTime);
        if (string.IsNullOrWhiteSpace(cutoffTime))
        {
            return true;
        }

        return !TimeOnly.TryParse(cutoffTime, out var eventTime) ||
               eventTime >= TimeOnly.FromDateTime(now);
    }

    private void DeleteBirthdayEventsForMember(int memberId)
    {
        foreach (var plannerEvent in _events.Find(x => x.SourceType == BirthdaySourceType && x.SourceMemberId == memberId).ToList())
        {
            _events.Delete(plannerEvent.Id);
        }
    }
}
