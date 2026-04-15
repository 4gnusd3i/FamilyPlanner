using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class EventWorkflowTests : DesktopPlannerUiTestBase
{
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
