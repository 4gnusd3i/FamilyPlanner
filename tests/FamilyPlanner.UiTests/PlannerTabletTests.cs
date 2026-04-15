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
        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(6));
        Assert.That(await GetGridColumnCountAsync(".week-days"), Is.EqualTo(7));
        Assert.That(await GetGridColumnCountAsync(".meals-grid"), Is.EqualTo(7));

        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".main-container");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");
        await AssertAllMinimumSizeAsync(".week-nav-btn", 44, 44);
        await AssertMinimumSizeAsync(".add-day-btn", 44, 44);

        await Expect(Page.Locator(".budget-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".shopping-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".side-panel .notes-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".upcoming-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".medicine-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".family-bar")).ToBeVisibleAsync();
        await Expect(Page.Locator(".planner-toolbar")).ToBeVisibleAsync();
        await Expect(Page.Locator("#shoppingList")).ToContainTextAsync("Vi har det vi trenger!");
        await Expect(Page.Locator("#upcomingList")).ToContainTextAsync("Ingen kommende avtaler");
        await Expect(Page.Locator("#medicineList .empty-state-collapsible")).ToBeHiddenAsync();
        await Expect(Page.Locator("#notesList .empty-state-collapsible")).ToBeHiddenAsync();

        Assert.That(await Page.GetByText("I DAG").CountAsync(), Is.EqualTo(0));

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
                const medicine = rect('.medicine-card');
                const family = rect('.family-bar');
                const lastAvatar = Array.from(document.querySelectorAll('.family-avatar')).at(-1).getBoundingClientRect();
                const addMember = document.querySelector('.add-family-btn').getBoundingClientRect();
                const quickActions = getComputedStyle(document.querySelector('.quick-actions'));
                const budgetDisplay = document.querySelector('.budget-display').getBoundingClientRect();
                const workspace = rect('.main-container');
                return [
                    budget[2],
                    notes[2],
                    planner[0],
                    planner[2],
                    upcoming[0],
                    medicine[0],
                    workspace[3],
                    family[1],
                    document.documentElement.scrollHeight,
                    window.innerHeight,
                    Math.abs(lastAvatar.top - addMember.top),
                    addMember.left - lastAvatar.right,
                    quickActions.boxShadow === 'none' ? 1 : 0,
                    budget[3] - budget[1],
                    budgetDisplay.height,
                    medicine[3] - medicine[1],
                    upcoming[3] - upcoming[1],
                    notes[3] - notes[1]
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(layout[0], Is.LessThanOrEqualTo(layout[2]), "Budget should stay in the left widget column.");
            Assert.That(layout[1], Is.LessThanOrEqualTo(layout[2]), "Notes should stay in the left widget column.");
            Assert.That(layout[4], Is.GreaterThanOrEqualTo(layout[3]), "Upcoming should stay in the right widget column.");
            Assert.That(layout[5], Is.GreaterThanOrEqualTo(layout[3]), "Medicines should stay in the right widget column.");
            Assert.That(layout[7], Is.GreaterThanOrEqualTo(layout[6] - 1d), "Family strip should sit below the workspace.");
            Assert.That(layout[8], Is.LessThanOrEqualTo(layout[9] + 2d), "Tablet kiosk layout should fit the viewport without page-level vertical scrolling.");
            Assert.That(layout[10], Is.LessThanOrEqualTo(2d), "Family add button should stay on the avatar row.");
            Assert.That(layout[11], Is.GreaterThanOrEqualTo(-1d), "Family add button should tail the avatars.");
            Assert.That(layout[12], Is.EqualTo(1d), "Quick actions should not keep the outer card shadow in kiosk mode.");
            Assert.That(layout[13], Is.GreaterThanOrEqualTo(layout[14]), "Budget card should not shrink below its content.");
            Assert.That(layout[15], Is.LessThan(layout[16]), "Empty medicines should stay compact below upcoming.");
            Assert.That(layout[17], Is.LessThan(layout[13]), "Empty notes should stay compact below the protected budget card.");
        });

        await OpenModalAsync("Nytt måltid", "mealModal");
        await Expect(Page.Locator("#mealModal")).ToBeVisibleAsync();
        await Expect(Page.Locator("#mealName")).ToBeVisibleAsync();
        await AssertModalFitsViewportAsync("mealModal");

        await Page.GetByRole(AriaRole.Button, new() { Name = "Avbryt", Exact = true }).First.ClickAsync();
        await WaitForModalStateAsync("mealModal", open: false);
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

    private async Task ClearKioskOptionalItemsAsync()
    {
        using var client = await UiTestHost.CreateClientAsync();

        foreach (var medicine in await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [])
        {
            var response = await client.PostAsJsonAsync("/api/medicines", new { delete = true, id = medicine.Id });
            response.EnsureSuccessStatusCode();
        }

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

        foreach (var entry in await GetApiAsync<List<PlannerEventDto>>("/api/events?upcoming=1") ?? [])
        {
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
}
