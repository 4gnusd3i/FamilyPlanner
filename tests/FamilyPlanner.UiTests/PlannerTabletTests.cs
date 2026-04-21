using System.Net.Http.Json;
using Microsoft.Playwright;
using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class PlannerTabletTests : PlannerUiTestBase
{
    protected override int ViewportWidth => 1024;
    protected override int ViewportHeight => 768;

    [Test]
    public async Task TabletLandscapePlanner_UsesKioskDashboardGrid()
    {
        await ClearKioskOptionalItemsAsync();
        await Page.ReloadAsync();
        await Expect(Page.Locator(".quick-actions")).ToBeVisibleAsync();
        await AssertNoHorizontalOverflowAsync();

        Assert.That(await GetGridColumnCountAsync(".main-container"), Is.EqualTo(3));
        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(5));
        Assert.That(await GetGridColumnCountAsync(".week-days"), Is.EqualTo(7));
        Assert.That(await GetGridColumnCountAsync(".meals-grid"), Is.EqualTo(7));
        await Expect(Page.Locator(".meal-day-header")).ToHaveCountAsync(0);

        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".main-container");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");
        await AssertAllMinimumSizeAsync(".week-nav-btn", 44, 44);

        await Expect(Page.Locator(".budget-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".shopping-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".side-panel .notes-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".upcoming-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".family-bar")).ToBeVisibleAsync();
        await Expect(Page.Locator(".planner-toolbar")).ToBeVisibleAsync();
        await Expect(Page.Locator("#shoppingList")).ToContainTextAsync("Vi har det vi trenger!");
        await Expect(Page.Locator("#upcomingList")).ToContainTextAsync("Anna har bursdag");
        await Expect(Page.Locator("#notesList .empty-state-collapsible")).ToBeHiddenAsync();
        await Expect(Page.Locator(".family-name", new() { HasTextString = "Anna" })).ToBeVisibleAsync();
        await Expect(Page.Locator(".side-panel-right .surface")).ToHaveCountAsync(1);

        var layout = await Page.EvaluateAsync<double[]>(
            @"() => {
                const rect = (selector) => {
                    const box = document.querySelector(selector).getBoundingClientRect();
                    return [box.left, box.top, box.right, box.bottom];
                };
                const budget = rect('.budget-card');
                const notes = rect('.notes-card');
                const planner = rect('.planner-surface');
                const upcoming = rect('.upcoming-card');
                const family = rect('.family-bar');
                const lastAvatar = Array.from(document.querySelectorAll('.family-avatar')).at(-1).getBoundingClientRect();
                const addMember = document.querySelector('.add-family-btn').getBoundingClientRect();
                const quickActions = getComputedStyle(document.querySelector('.quick-actions'));
                const familyStyles = getComputedStyle(document.querySelector('.family-bar'));
                const budgetDisplay = document.querySelector('.budget-display').getBoundingClientRect();
                const workspace = rect('.main-container');
                return [
                    budget[2],
                    notes[2],
                    planner[0],
                    planner[2],
                    upcoming[0],
                    workspace[3],
                    family[1],
                    document.documentElement.scrollHeight,
                    window.innerHeight,
                    Math.abs(lastAvatar.top - addMember.top),
                    Math.abs(lastAvatar.left - addMember.left),
                    quickActions.boxShadow === 'none' ? 1 : 0,
                    budget[3] - budget[1],
                    budgetDisplay.height,
                    notes[3],
                    familyStyles.boxShadow === 'none' ? 1 : 0,
                    familyStyles.backgroundColor === 'rgba(0, 0, 0, 0)' ? 1 : 0
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(layout[0], Is.LessThanOrEqualTo(layout[2]), "Budget should stay in the left widget column.");
            Assert.That(layout[1], Is.LessThanOrEqualTo(layout[2]), "Notes should stay in the left widget column.");
            Assert.That(layout[4], Is.GreaterThanOrEqualTo(layout[3]), "Upcoming should stay in the right widget column.");
            Assert.That(layout[6], Is.GreaterThanOrEqualTo(layout[5] - 1d), "Family strip should sit below the workspace.");
            Assert.That(layout[7], Is.LessThanOrEqualTo(layout[8] + 2d), "Tablet kiosk layout should fit the viewport without page-level vertical scrolling.");
            Assert.That(layout[9], Is.LessThanOrEqualTo(2d), "Family add button should stay aligned with avatar tiles.");
            Assert.That(layout[10], Is.GreaterThan(0d), "Family add button should remain a distinct tile after the member tiles.");
            Assert.That(layout[11], Is.EqualTo(1d), "Quick actions should not keep the outer card shadow in kiosk mode.");
            Assert.That(layout[12], Is.GreaterThanOrEqualTo(layout[13]), "Budget card should not shrink below its content.");
            Assert.That(layout[14], Is.LessThanOrEqualTo(layout[6] + 1d), "Notes should not overlap the family strip.");
            Assert.That(layout[15], Is.EqualTo(1d), "Family strip should not keep an outer card shadow in kiosk mode.");
            Assert.That(layout[16], Is.EqualTo(1d), "Family strip should be visually transparent in kiosk mode.");
        });

        await OpenModalBySelectorAsync(".quick-action:has-text('Måltid')", "mealModal");
        await Expect(Page.Locator("#mealModal")).ToBeVisibleAsync();
        await Expect(Page.Locator("#mealName")).ToBeVisibleAsync();
        await AssertModalFitsViewportAsync("mealModal");

        await Page.GetByRole(AriaRole.Button, new() { Name = "Avbryt", Exact = true }).First.ClickAsync();
        await WaitForModalStateAsync("mealModal", open: false);
    }

    [Test]
    public async Task TabletLandscape_CalendarEventItemsStayContainedWithStructuredLines()
    {
        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var family = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var annaId = family.Single(x => x.Name == "Anna").Id;
        const string longTitle = "Ekstra lang avtaletittel som skal kuttes rent uten aa stikke ut av kalenderdagen";
        const string longDescription = "Dette er en lang beskrivelse som skal holde seg inne i kortet, klampes rent og ikke dytte resten av oppsettet ut av kurs.";

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = longTitle,
            ["event_date"] = today,
            ["start_time"] = "17:10",
            ["end_time"] = "18:10",
            ["owner_id"] = annaId.ToString(),
            ["note"] = longDescription,
        });

        for (var idx = 1; idx <= 8; idx += 1)
        {
            await PostFormAsync("/api/events", new Dictionary<string, string>
            {
                ["title"] = $"Tett avtale {idx}",
                ["event_date"] = today,
                ["start_time"] = $"1{idx % 10}:00",
                ["end_time"] = $"1{idx % 10}:30",
                ["owner_id"] = annaId.ToString(),
                ["note"] = $"Kort notat {idx}",
            });
        }

        await Page.ReloadAsync();

        var longEvent = Page.Locator(".event-item", new() { HasTextString = longTitle });
        await Expect(longEvent).ToBeVisibleAsync();

        var layout = await longEvent.EvaluateAsync<double[]>(
            @"item => {
                const day = item.closest('.day-box');
                const dayContent = day.querySelector('.day-content');
                const owner = item.querySelector('.event-owner-line');
                const title = item.querySelector('.event-title');
                const time = item.querySelector('.event-time');
                const description = item.querySelector('.event-description');
                const itemRect = item.getBoundingClientRect();
                const dayRect = day.getBoundingClientRect();
                const titleRect = title.getBoundingClientRect();
                const timeRect = time.getBoundingClientRect();
                const descriptionRect = description.getBoundingClientRect();
                return [
                    itemRect.left - dayRect.left,
                    dayRect.right - itemRect.right,
                    owner.getBoundingClientRect().top,
                    titleRect.top,
                    timeRect.top,
                    descriptionRect.top,
                    title.scrollWidth > title.clientWidth ? 1 : 0,
                    description.scrollHeight > description.clientHeight ? 1 : 0,
                    descriptionRect.right - itemRect.right,
                    dayContent.scrollHeight > dayContent.clientHeight ? 1 : 0
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(layout[0], Is.GreaterThanOrEqualTo(-1d), "Event item should not extend past the left edge of its day.");
            Assert.That(layout[1], Is.GreaterThanOrEqualTo(-1d), "Event item should not extend past the right edge of its day.");
            Assert.That(layout[2], Is.LessThan(layout[3]), "Owner line should render above the title line.");
            Assert.That(layout[3], Is.LessThan(layout[4]), "Title line should render above the time line.");
            Assert.That(layout[4], Is.LessThan(layout[5]), "Time line should render above the description line.");
            Assert.That(layout[6], Is.EqualTo(1d), "Long event titles should ellipsize inside the event item.");
            Assert.That(layout[7], Is.EqualTo(1d), "Long descriptions should clamp inside the event item.");
            Assert.That(layout[8], Is.LessThanOrEqualTo(1d), "Event description should stay inside the event item.");
            Assert.That(layout[9], Is.EqualTo(1d), "Dense calendar days should scroll internally instead of cutting entries.");
        });
    }

    [Test]
    public async Task TabletLandscape_LeftColumnShowsSingleItemsWithoutScroll()
    {
        await ClearKioskOptionalItemsAsync();
        await AddSingleLeftColumnItemsAsync();
        await Page.ReloadAsync();

        await Expect(Page.Locator("#shoppingList")).ToContainTextAsync("Kontantkort");
        await Expect(Page.Locator("#notesList")).ToContainTextAsync("SFO");

        var overflow = await Page.EvaluateAsync<double[]>(
            @"() => {
                const shopping = document.querySelector('#shoppingList');
                const notes = document.querySelector('#notesList');
                return [
                    shopping.scrollHeight - shopping.clientHeight,
                    notes.scrollHeight - notes.clientHeight
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(overflow[0], Is.LessThanOrEqualTo(1d), "Handleliste should show one item without scrolling.");
            Assert.That(overflow[1], Is.LessThanOrEqualTo(1d), "Notater should show one item without scrolling.");
        });
    }

    [Test]
    [TestCase(4)]
    [TestCase(6)]
    public async Task TabletLandscape_ShoppingStaysVisibleWhenNotesGrow(int noteCount)
    {
        await ClearKioskOptionalItemsAsync();
        await AddDenseNotesWithSingleShoppingAsync(noteCount);
        await Page.ReloadAsync();

        await Expect(Page.Locator("#shoppingList")).ToContainTextAsync("Kontantkort");
        await Expect(Page.Locator("#notesList")).ToContainTextAsync($"Notat {noteCount}");

        var shoppingLayout = await Page.EvaluateAsync<double[]>(
            @"() => {
                const list = document.querySelector('#shoppingList');
                const first = list.querySelector('.shop-item');
                const listRect = list.getBoundingClientRect();
                const firstRect = first.getBoundingClientRect();
                const shoppingCardRect = document.querySelector('.shopping-card').getBoundingClientRect();
                const notesCardRect = document.querySelector('.notes-card').getBoundingClientRect();
                const budgetCardRect = document.querySelector('.budget-card').getBoundingClientRect();
                const budgetDisplayRect = document.querySelector('.budget-display').getBoundingClientRect();
                return [
                    list.scrollHeight - list.clientHeight,
                    firstRect.top - listRect.top,
                    listRect.bottom - firstRect.bottom,
                    shoppingCardRect.height,
                    notesCardRect.height,
                    budgetCardRect.height,
                    budgetDisplayRect.height
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(shoppingLayout[0], Is.LessThanOrEqualTo(1d), "Handleliste should keep one item visible without scroll even when notes grow.");
            Assert.That(shoppingLayout[1], Is.GreaterThanOrEqualTo(-1d), "Handleliste item should stay fully visible from the top.");
            Assert.That(shoppingLayout[2], Is.GreaterThanOrEqualTo(-1d), "Handleliste item should stay fully visible at the bottom.");
            Assert.That(shoppingLayout[3], Is.GreaterThanOrEqualTo(shoppingLayout[4] - 4d), "Handleliste should keep layout priority and remain at least as tall as Notater within layout rounding tolerance.");
            Assert.That(shoppingLayout[5], Is.GreaterThanOrEqualTo(shoppingLayout[6] - 1d), "Budsjett should stay fixed to its own content needs.");
        });
    }

    private async Task ClearKioskOptionalItemsAsync()
    {
        using var client = await UiTestHost.CreateClientAsync();

        foreach (var note in await GetApiAsync<List<NoteItemDto>>("/api/notes") ?? [])
        {
            var response = await client.PostAsJsonAsync("/api/notes", new { delete = true, id = note.Id });
            response.EnsureSuccessStatusCode();
        }

        foreach (var item in await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [])
        {
            var response = await client.PostAsJsonAsync("/api/shopping", new { delete = true, id = item.Id });
            response.EnsureSuccessStatusCode();
        }

        var start = DateTime.Today.AddDays(-14).ToString("yyyy-MM-dd");
        var end = DateTime.Today.AddDays(42).ToString("yyyy-MM-dd");
        foreach (var entry in await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={start}&end={end}") ?? [])
        {
            if (entry.SourceType == "birthday") continue;
            var response = await client.PostAsJsonAsync("/api/events", new { delete = true, id = entry.Id });
            response.EnsureSuccessStatusCode();
        }
    }

    private async Task AddSingleLeftColumnItemsAsync()
    {
        using var client = await UiTestHost.CreateClientAsync();

        var noteResponse = await client.PostAsync(
            "/api/notes",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["title"] = "SFO",
                ["content"] = "Torsdag hentes 16:00",
            }));
        noteResponse.EnsureSuccessStatusCode();

        var shoppingResponse = await client.PostAsync(
            "/api/shopping",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["item"] = "Kontantkort",
                ["quantity"] = "1",
            }));
        shoppingResponse.EnsureSuccessStatusCode();
    }

    private async Task AddDenseNotesWithSingleShoppingAsync(int noteCount)
    {
        using var client = await UiTestHost.CreateClientAsync();

        foreach (var idx in Enumerable.Range(1, noteCount))
        {
            var noteResponse = await client.PostAsync(
                "/api/notes",
                new FormUrlEncodedContent(new Dictionary<string, string>
                {
                    ["title"] = $"Notat {idx}",
                    ["content"] = $"Innhold {idx}",
                }));
            noteResponse.EnsureSuccessStatusCode();
        }

        var shoppingResponse = await client.PostAsync(
            "/api/shopping",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["item"] = "Kontantkort",
                ["quantity"] = "1",
            }));
        shoppingResponse.EnsureSuccessStatusCode();
    }
}
