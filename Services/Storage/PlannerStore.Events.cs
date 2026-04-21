using System.Globalization;
using FamilyPlanner.Models;

namespace FamilyPlanner.Services.Storage;

public sealed partial class PlannerStore
{
    private const string BirthdaySourceType = "birthday";
    private const string RecurringSourceType = "recurring";
    private const int UpcomingWindowDays = 2;
    private const string DefaultEventColor = "#eaf4ff";

    public IReadOnlyList<PlannerEvent> GetEvents(DateOnly start, DateOnly end)
    {
        EnsureBirthdayEvents(start, end);

        var directEvents = _events.FindAll()
            .Where(x => !IsRecurringSeries(x) && IsWithinRange(x.EventDate, start, end))
            .Select(CloneEvent)
            .ToList();

        return directEvents
            .Concat(ExpandRecurringEvents(start, end))
            .Select(ApplyCurrentEventColor)
            .OrderBy(x => x.EventDate)
            .ThenBy(GetEventSortTime)
            .ThenBy(x => x.Title)
            .ToList();
    }

    public IReadOnlyList<PlannerEvent> GetUpcomingEvents(DateOnly fromDate)
    {
        var now = DateTime.Now;
        var windowEnd = fromDate.AddDays(UpcomingWindowDays);
        EnsureBirthdayEvents(fromDate, windowEnd);

        var directEvents = _events.FindAll()
            .Where(x => !IsRecurringSeries(x))
            .Select(CloneEvent)
            .Where(x => IsUpcomingEvent(x, now, windowEnd))
            .ToList();

        return directEvents
            .Concat(ExpandRecurringEvents(fromDate, windowEnd).Where(x => IsUpcomingEvent(x, now, windowEnd)))
            .Select(ApplyCurrentEventColor)
            .OrderBy(x => x.EventDate)
            .ThenBy(GetEventSortTime)
            .ThenBy(x => x.Title)
            .ToList();
    }

    public PlannerEvent UpsertEvent(
        int? id,
        string title,
        string eventDate,
        string? startTime,
        string? endTime,
        string? recurrenceType,
        string? recurrenceUntil,
        int? ownerId,
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
        entity.RecurrenceType = NormalizeOptional(recurrenceType);
        entity.RecurrenceUntil = NormalizeOptional(recurrenceUntil);
        entity.OwnerId = ownerId;
        entity.Color = ResolveEventColor(ownerId);
        entity.Note = NormalizeOptional(note);
        entity.SeriesStartDate = null;

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
                Note = "F\u00f8dselsdag",
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
        existing.Note = "F\u00f8dselsdag";
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

    private static bool IsUpcomingEvent(PlannerEvent plannerEvent, DateTime now, DateOnly windowEnd)
    {
        if (!DateOnly.TryParse(plannerEvent.EventDate, out var eventDate))
        {
            return false;
        }

        var today = DateOnly.FromDateTime(now);
        if (eventDate > windowEnd)
        {
            return false;
        }

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

    private PlannerEvent ApplyCurrentEventColor(PlannerEvent plannerEvent)
    {
        plannerEvent.Color = ResolveEventColor(plannerEvent.OwnerId);
        return plannerEvent;
    }

    private string ResolveEventColor(int? ownerId)
    {
        if (ownerId is > 0 && _familyMembers.FindById(ownerId.Value) is { } member && !string.IsNullOrWhiteSpace(member.Color))
        {
            return member.Color;
        }

        return DefaultEventColor;
    }

    private void DeleteBirthdayEventsForMember(int memberId)
    {
        foreach (var plannerEvent in _events.Find(x => x.SourceType == BirthdaySourceType && x.SourceMemberId == memberId).ToList())
        {
            _events.Delete(plannerEvent.Id);
        }
    }

    private IEnumerable<PlannerEvent> ExpandRecurringEvents(DateOnly rangeStart, DateOnly rangeEnd)
    {
        if (rangeEnd < rangeStart)
        {
            yield break;
        }

        foreach (var series in _events.FindAll().Where(IsRecurringSeries))
        {
            if (!DateOnly.TryParse(series.EventDate, out var seriesStart))
            {
                continue;
            }

            var recurrenceEnd = DateOnly.TryParse(series.RecurrenceUntil, out var parsedRecurrenceEnd)
                ? parsedRecurrenceEnd
                : rangeEnd;
            if (recurrenceEnd < rangeStart || recurrenceEnd < seriesStart)
            {
                continue;
            }

            var firstOccurrence = GetFirstOccurrenceOnOrAfter(seriesStart, series.RecurrenceType!, rangeStart);
            var effectiveEnd = recurrenceEnd < rangeEnd ? recurrenceEnd : rangeEnd;
            for (var occurrence = firstOccurrence; occurrence <= effectiveEnd; occurrence = NextOccurrence(occurrence, series.RecurrenceType!))
            {
                if (occurrence < seriesStart)
                {
                    continue;
                }

                yield return CreateRecurringOccurrence(series, occurrence);
            }
        }
    }

    private static PlannerEvent CloneEvent(PlannerEvent plannerEvent) => new()
    {
        Id = plannerEvent.Id,
        UserId = plannerEvent.UserId,
        CreatedAt = plannerEvent.CreatedAt,
        Title = plannerEvent.Title,
        EventDate = plannerEvent.EventDate,
        StartTime = plannerEvent.StartTime,
        EndTime = plannerEvent.EndTime,
        RecurrenceType = plannerEvent.RecurrenceType,
        RecurrenceUntil = plannerEvent.RecurrenceUntil,
        OwnerId = plannerEvent.OwnerId,
        Color = plannerEvent.Color,
        Note = plannerEvent.Note,
        SourceType = plannerEvent.SourceType,
        SourceMemberId = plannerEvent.SourceMemberId,
        SourceYear = plannerEvent.SourceYear,
        SeriesStartDate = plannerEvent.SeriesStartDate
    };

    private static PlannerEvent CreateRecurringOccurrence(PlannerEvent series, DateOnly occurrenceDate) => new()
    {
        Id = series.Id,
        UserId = series.UserId,
        CreatedAt = series.CreatedAt,
        Title = series.Title,
        EventDate = occurrenceDate.ToString("yyyy-MM-dd"),
        StartTime = series.StartTime,
        EndTime = series.EndTime,
        RecurrenceType = series.RecurrenceType,
        RecurrenceUntil = series.RecurrenceUntil,
        OwnerId = series.OwnerId,
        Color = series.Color,
        Note = series.Note,
        SourceType = RecurringSourceType,
        SeriesStartDate = series.EventDate
    };

    private static bool IsRecurringSeries(PlannerEvent plannerEvent) =>
        string.Equals(plannerEvent.RecurrenceType, "daily", StringComparison.Ordinal) ||
        string.Equals(plannerEvent.RecurrenceType, "weekly", StringComparison.Ordinal);

    private static bool IsWithinRange(string? eventDate, DateOnly start, DateOnly end) =>
        !string.IsNullOrWhiteSpace(eventDate) &&
        string.CompareOrdinal(eventDate, start.ToString("yyyy-MM-dd")) >= 0 &&
        string.CompareOrdinal(eventDate, end.ToString("yyyy-MM-dd")) <= 0;

    private static string GetEventSortTime(PlannerEvent plannerEvent) =>
        NormalizeOptional(plannerEvent.EndTime) ??
        NormalizeOptional(plannerEvent.StartTime) ??
        "99:99:99";

    private static DateOnly GetFirstOccurrenceOnOrAfter(DateOnly seriesStart, string recurrenceType, DateOnly rangeStart)
    {
        if (rangeStart <= seriesStart)
        {
            return seriesStart;
        }

        return recurrenceType switch
        {
            "daily" => rangeStart,
            "weekly" => seriesStart.AddDays((((rangeStart.DayNumber - seriesStart.DayNumber) + 6) / 7) * 7),
            _ => seriesStart
        };
    }

    private static DateOnly NextOccurrence(DateOnly occurrence, string recurrenceType) =>
        recurrenceType switch
        {
            "daily" => occurrence.AddDays(1),
            "weekly" => occurrence.AddDays(7),
            _ => occurrence.AddDays(1)
        };
}
