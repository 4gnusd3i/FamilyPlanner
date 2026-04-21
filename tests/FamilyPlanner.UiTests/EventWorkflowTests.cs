using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class EventWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task EventModal_DefaultTimes_AreNowPlusOneHour_AndStartSyncsEndOneWay()
    {
        await OpenModalBySelectorAsync(".quick-action:has-text('Avtale')", "eventModal");

        var defaultTimes = await Page.EvaluateAsync<int[]>(
            @"() => {
                const toMinutes = (value) => {
                    const [h, m] = value.split(':').map(Number);
                    return (h * 60) + m;
                };
                const start = document.querySelector('#eventStartTime').value;
                const end = document.querySelector('#eventEndTime').value;
                const now = new Date();
                const nowMinutes = (now.getHours() * 60) + now.getMinutes();
                const startMinutes = toMinutes(start);
                const endMinutes = toMinutes(end);
                const plusOneHour = (startMinutes + 60) % (24 * 60);
                return [startMinutes, endMinutes, nowMinutes, plusOneHour];
            }");

        Assert.Multiple(() =>
        {
          Assert.That(defaultTimes[0], Is.InRange(defaultTimes[2] - 2, defaultTimes[2] + 2), "Start time should default to current local time.");
          Assert.That(defaultTimes[1], Is.EqualTo(defaultTimes[3]), "End time should default to one hour after start.");
        });

        await Page.Locator("#eventStartTime").FillAsync("09:10");
        await Expect(Page.Locator("#eventEndTime")).ToHaveValueAsync("10:10");

        await Page.EvaluateAsync(
            @"() => {
                const start = document.querySelector('#eventStartTime');
                start.value = '11:20';
                start.dispatchEvent(new Event('change', { bubbles: true }));
            }");
        await Expect(Page.Locator("#eventEndTime")).ToHaveValueAsync("12:20");

        await Page.Locator("#eventEndTime").FillAsync("12:45");
        await Expect(Page.Locator("#eventStartTime")).ToHaveValueAsync("11:20");

        await Page.Locator("#eventEndTime").FillAsync(string.Empty);
        await Page.Locator("#eventEndTime").DispatchEventAsync("pointerdown");
        await Expect(Page.Locator("#eventEndTime")).ToHaveValueAsync("12:20");
    }

    [Test]
    public async Task EventCrud_PersistsThroughUiApiAndStore()
    {
        var (start, end) = GetCurrentWeekRange();
        var family = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var anna = family.Single(x => x.Name == "Anna");
        var initialEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        var initialCount = initialEvents.Count;

        await OpenModalBySelectorAsync(".quick-action:has-text('Avtale')", "eventModal");
        await Page.Locator("#eventTitle").FillAsync("Parent meeting");
        await Page.Locator("#eventDate").FillAsync(DateTime.Today.ToString("yyyy-MM-dd"));
        await Page.Locator("#eventStartTime").FillAsync("19:00");
        await Page.Locator("#eventEndTime").FillAsync("20:00");
        await Page.Locator("#eventOwner").SelectOptionAsync(anna.Id.ToString());
        await Page.Locator("#eventNote").FillAsync("Bring the schedule.");
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        await Expect(Page.Locator(".event-item", new() { HasTextString = "Parent meeting" })).ToBeVisibleAsync();

        var createdEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        var createdEvent = createdEvents.Single(x => x.Title == "Parent meeting");
        Assert.That(createdEvent.Color, Is.EqualTo(anna.Color));

        await Page.Locator(".event-item", new() { HasTextString = "Parent meeting" }).ClickAsync();
        await WaitForModalStateAsync("eventModal", open: true);
        await Page.Locator("#eventTitle").FillAsync("Updated parent meeting");
        await Page.Locator("#eventNote").FillAsync("Room 3B.");
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        var updatedEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        var updatedEvent = updatedEvents.Single(x => x.Id == createdEvent.Id);
        Assert.That(updatedEvent.Title, Is.EqualTo("Updated parent meeting"));
        Assert.That(updatedEvent.Note, Is.EqualTo("Room 3B."));

        await Page.Locator(".event-item", new() { HasTextString = "Updated parent meeting" }).ClickAsync();
        await WaitForModalStateAsync("eventModal", open: true);
        await Page.Locator("#deleteEventBtn").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        await Expect(Page.Locator(".event-item", new() { HasTextString = "Updated parent meeting" })).ToHaveCountAsync(0);

        var finalEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        Assert.That(finalEvents.Any(x => x.Id == createdEvent.Id), Is.False);
        Assert.That(finalEvents, Has.Count.EqualTo(initialCount));
    }

    [Test]
    public async Task RecurringEventCrud_UsesWholeSeriesScope()
    {
        var (start, end) = GetCurrentWeekRange();
        var recurrenceEnd = DateTime.Today.AddDays(2).ToString("yyyy-MM-dd");

        await OpenModalBySelectorAsync(".quick-action:has-text('Avtale')", "eventModal");
        await Page.Locator("#eventTitle").FillAsync("Lekser");
        await Page.Locator("#eventDate").FillAsync(DateTime.Today.ToString("yyyy-MM-dd"));
        await Page.Locator("#eventStartTime").FillAsync("18:00");
        await Page.Locator("#eventEndTime").FillAsync("18:45");
        await Page.Locator("#eventRecurrenceType").SelectOptionAsync("daily");
        await Page.Locator("#eventRecurrenceUntil").FillAsync(recurrenceEnd);
        await Page.Locator("#eventNote").FillAsync("Stuebordet.");
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        var createdEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        var recurringEvents = createdEvents.Where(x => x.Title == "Lekser").ToList();

        Assert.Multiple(() =>
        {
            Assert.That(recurringEvents, Has.Count.EqualTo(3));
            Assert.That(recurringEvents.All(x => x.SourceType == "recurring"), Is.True);
        });

        await Page.Locator(".event-item", new() { HasTextString = "Lekser" }).Nth(1).ClickAsync();
        await WaitForModalStateAsync("eventModal", open: true);
        await Expect(Page.Locator("#eventDate")).ToHaveValueAsync(DateTime.Today.ToString("yyyy-MM-dd"));
        await Page.Locator("#eventNote").FillAsync("Oppdatert serie.");
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        var updatedEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        Assert.That(updatedEvents.Where(x => x.Title == "Lekser").All(x => x.Note == "Oppdatert serie."), Is.True);

        await Page.Locator(".event-item", new() { HasTextString = "Lekser" }).First.ClickAsync();
        await WaitForModalStateAsync("eventModal", open: true);
        await AcceptDialogAsync(
            () => Page.Locator("#deleteEventBtn").ClickAsync(),
            "gjentakende serien");
        await WaitForModalStateAsync("eventModal", open: false);

        var finalEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        Assert.That(finalEvents.Any(x => x.Title == "Lekser"), Is.False);
    }

    [Test]
    public async Task Upcoming_UsesThreeCalendarDays_AndKeepsSameDayUntimedEventsUntilTomorrow()
    {
        var now = DateTime.Now;
        var pastMoment = now.TimeOfDay > TimeSpan.FromMinutes(5)
            ? now.AddMinutes(-5)
            : now.Date.AddDays(-1).AddHours(22);
        var pastEnd = pastMoment.AddMinutes(1);

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Already finished",
            ["event_date"] = pastMoment.ToString("yyyy-MM-dd"),
            ["start_time"] = pastMoment.ToString("HH:mm"),
            ["end_time"] = pastEnd.ToString("HH:mm"),
        });

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Tomorrow appointment",
            ["event_date"] = now.Date.AddDays(1).ToString("yyyy-MM-dd"),
            ["start_time"] = "12:00",
            ["end_time"] = "13:00",
        });

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Same day untimed",
            ["event_date"] = now.Date.ToString("yyyy-MM-dd"),
        });

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Day after appointment",
            ["event_date"] = now.Date.AddDays(2).ToString("yyyy-MM-dd"),
            ["start_time"] = "09:00",
            ["end_time"] = "10:00",
        });

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Outside three day window",
            ["event_date"] = now.Date.AddDays(3).ToString("yyyy-MM-dd"),
            ["start_time"] = "09:00",
            ["end_time"] = "10:00",
        });

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Training pickup",
            ["event_date"] = now.Date.ToString("yyyy-MM-dd"),
            ["start_time"] = now.AddMinutes(20).ToString("HH:mm"),
            ["end_time"] = now.AddMinutes(40).ToString("HH:mm"),
            ["recurrence_type"] = "weekly",
        });

        var upcomingEvents = await GetApiAsync<List<PlannerEventDto>>("/api/events?upcoming=1") ?? [];
        Assert.Multiple(() =>
        {
            Assert.That(upcomingEvents.Any(x => x.Title == "Already finished"), Is.False);
            Assert.That(upcomingEvents.Any(x => x.Title == "Tomorrow appointment"), Is.True);
            Assert.That(upcomingEvents.Any(x => x.Title == "Same day untimed"), Is.True);
            Assert.That(upcomingEvents.Any(x => x.Title == "Day after appointment"), Is.True);
            Assert.That(upcomingEvents.Any(x => x.Title == "Outside three day window"), Is.False);
            Assert.That(upcomingEvents.Any(x => x.Title == "Training pickup" && x.SourceType == "recurring"), Is.True);
            Assert.That(
                upcomingEvents.Any(x =>
                    x.SourceType == "birthday" &&
                    x.Title == "Anna har bursdag" &&
                    x.EventDate == DateTime.Today.ToString("yyyy-MM-dd")),
                Is.True);
        });
    }

    [Test]
    public async Task UpcomingList_RendersOwnerNameBesideAvatar()
    {
        var family = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var oskar = family.Single(x => x.Name == "Oskar");

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Bibliotekbesok",
            ["event_date"] = DateTime.Today.AddDays(1).ToString("yyyy-MM-dd"),
            ["start_time"] = "12:00",
            ["end_time"] = "12:30",
            ["owner_id"] = oskar.Id.ToString(),
        });

        await Page.ReloadAsync();

        var upcomingItem = Page.Locator(".upcoming-item", new() { HasTextString = "Bibliotekbesok" });
        await Expect(upcomingItem).ToBeVisibleAsync();
        await Expect(upcomingItem.Locator(".member-avatar")).ToBeVisibleAsync();
        await Expect(upcomingItem.Locator(".upcoming-owner-name")).ToHaveTextAsync("Oskar");
    }

    private static (string Start, string End) GetCurrentWeekRange()
    {
        var today = DateTime.Today;
        var monday = today.AddDays(-(((int)today.DayOfWeek + 6) % 7));
        var sunday = monday.AddDays(6);
        return (monday.ToString("yyyy-MM-dd"), sunday.ToString("yyyy-MM-dd"));
    }
}
