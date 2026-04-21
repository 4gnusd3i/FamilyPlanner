using System.Net;
using System.Net.Http.Json;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class RecurrenceApiTests
{
    [SetUp]
    public async Task SetUpAsync() => await UiTestHost.ResetStateAsync();

    [Test]
    public async Task CalendarRange_ExpandsRecurringSeries_UsingParentSeriesId()
    {
        using var client = await UiTestHost.CreateClientAsync();
        var startDate = DateOnly.FromDateTime(DateTime.Today);
        var endDate = startDate.AddDays(10);

        using var createResponse = await client.PostAsync(
            "/api/events",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "Leksehjelp",
                ["event_date"] = startDate.ToString("yyyy-MM-dd"),
                ["start_time"] = "18:00",
                ["end_time"] = "19:00",
                ["recurrence_type"] = "daily",
                ["recurrence_until"] = startDate.AddDays(2).ToString("yyyy-MM-dd"),
            }));
        createResponse.EnsureSuccessStatusCode();

        var events = await client.GetFromJsonAsync<List<PlannerEventDto>>(
            $"/api/events?start={startDate:yyyy-MM-dd}&end={endDate:yyyy-MM-dd}") ?? [];
        var recurringEvents = events.Where(x => x.Title == "Leksehjelp").ToList();

        Assert.Multiple(() =>
        {
            Assert.That(recurringEvents, Has.Count.EqualTo(3));
            Assert.That(recurringEvents.Select(x => x.Id).Distinct().Count(), Is.EqualTo(1));
            Assert.That(recurringEvents.All(x => x.SourceType == "recurring"), Is.True);
            Assert.That(recurringEvents.Select(x => x.EventDate), Is.EqualTo(new[]
            {
                startDate.ToString("yyyy-MM-dd"),
                startDate.AddDays(1).ToString("yyyy-MM-dd"),
                startDate.AddDays(2).ToString("yyyy-MM-dd"),
            }));
            Assert.That(recurringEvents.All(x => x.SeriesStartDate == startDate.ToString("yyyy-MM-dd")), Is.True);
        });
    }

    [Test]
    public async Task Upcoming_AppliesRecurringAndSameDayVisibilityRules()
    {
        using var client = await UiTestHost.CreateClientAsync();
        var now = DateTime.Now;

        using var recurringResponse = await client.PostAsync(
            "/api/events",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "Hent barn",
                ["event_date"] = now.ToString("yyyy-MM-dd"),
                ["start_time"] = now.AddMinutes(20).ToString("HH:mm"),
                ["end_time"] = now.AddMinutes(50).ToString("HH:mm"),
                ["recurrence_type"] = "weekly",
            }));
        recurringResponse.EnsureSuccessStatusCode();

        using var untimedResponse = await client.PostAsync(
            "/api/events",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "Same-day untimed",
                ["event_date"] = now.ToString("yyyy-MM-dd"),
            }));
        untimedResponse.EnsureSuccessStatusCode();

        using var outsideWindowResponse = await client.PostAsync(
            "/api/events",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "Outside upcoming window",
                ["event_date"] = now.AddDays(3).ToString("yyyy-MM-dd"),
            }));
        outsideWindowResponse.EnsureSuccessStatusCode();

        var upcoming = await client.GetFromJsonAsync<List<PlannerEventDto>>("/api/events?upcoming=1") ?? [];

        Assert.Multiple(() =>
        {
            Assert.That(upcoming.Any(x => x.Title == "Hent barn" && x.SourceType == "recurring"), Is.True);
            Assert.That(upcoming.Any(x => x.Title == "Same-day untimed"), Is.True);
            Assert.That(upcoming.Any(x => x.Title == "Outside upcoming window"), Is.False);
            Assert.That(upcoming.Any(x => x.SourceType == "birthday" && x.Title == "Anna har bursdag"), Is.True);
        });
    }

    [Test]
    public async Task InvalidRecurrenceValues_AreRejected()
    {
        using var client = await UiTestHost.CreateClientAsync();

        using var invalidTypeResponse = await client.PostAsync(
            "/api/events",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "Ugyldig",
                ["event_date"] = DateTime.Today.ToString("yyyy-MM-dd"),
                ["recurrence_type"] = "monthly",
            }));

        using var invalidUntilResponse = await client.PostAsync(
            "/api/events",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "Ugyldig sluttdato",
                ["event_date"] = DateTime.Today.ToString("yyyy-MM-dd"),
                ["recurrence_type"] = "daily",
                ["recurrence_until"] = DateTime.Today.AddDays(-1).ToString("yyyy-MM-dd"),
            }));

        Assert.Multiple(() =>
        {
            Assert.That(invalidTypeResponse.StatusCode, Is.EqualTo(HttpStatusCode.BadRequest));
            Assert.That(invalidUntilResponse.StatusCode, Is.EqualTo(HttpStatusCode.BadRequest));
        });
    }
}
