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
    }

    [Test]
    public async Task EventCrud_PersistsThroughUiApiAndStore()
    {
        var (start, end) = GetCurrentWeekRange();
        var initialEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        var initialCount = initialEvents.Count;

        await OpenModalBySelectorAsync(".quick-action:has-text('Avtale')", "eventModal");
        await Page.Locator("#eventTitle").FillAsync("Parent meeting");
        await Page.Locator("#eventDate").FillAsync(DateTime.Today.ToString("yyyy-MM-dd"));
        await Page.Locator("#eventStartTime").FillAsync("19:00");
        await Page.Locator("#eventEndTime").FillAsync("20:00");
        await Page.Locator("#eventNote").FillAsync("Bring the schedule.");
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        await Expect(Page.Locator(".event-item", new() { HasTextString = "Parent meeting" })).ToBeVisibleAsync();

        var createdEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        var createdEvent = createdEvents.Single(x => x.Title == "Parent meeting");

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
        await AcceptDialogAsync(
            () => Page.Locator("#deleteEventBtn").ClickAsync(),
            "Slette");

        await Expect(Page.Locator(".event-item", new() { HasTextString = "Updated parent meeting" })).ToHaveCountAsync(0);

        var finalEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [];
        Assert.That(finalEvents.Any(x => x.Id == createdEvent.Id), Is.False);
        Assert.That(finalEvents, Has.Count.EqualTo(initialCount));
    }

    private static (string Start, string End) GetCurrentWeekRange()
    {
        var today = DateTime.Today;
        var monday = today.AddDays(-(((int)today.DayOfWeek + 6) % 7));
        var sunday = monday.AddDays(6);
        return (monday.ToString("yyyy-MM-dd"), sunday.ToString("yyyy-MM-dd"));
    }
}
