using Microsoft.Playwright;
using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class PlannerPhoneTests : PlannerUiTestBase
{
    protected override int ViewportWidth => 390;
    protected override int ViewportHeight => 844;

    [Test]
    public async Task PhonePlanner_UsesSingleColumnResponsiveLayout()
    {
        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(1));
        Assert.That(await GetGridColumnCountAsync(".week-days"), Is.EqualTo(1));
        Assert.That(await GetGridColumnCountAsync(".main-container"), Is.EqualTo(1));

        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");
        await AssertLocatorFitsViewportWidthAsync(".planner-toolbar");
        await AssertLocatorFitsViewportWidthAsync(".week-switcher");
        await AssertAllMinimumSizeAsync(".week-nav-btn", 44, 44);

        await Expect(Page.GetByRole(AriaRole.Button, new() { Name = "Nytt familiemedlem", Exact = true })).ToBeVisibleAsync();
        await Page.Locator(".quick-action", new() { HasTextString = "Måltid" }).First.ClickAsync();
        await Page.WaitForFunctionAsync(
            @"modal => document.getElementById(modal)?.classList.contains('active') === true",
            "mealModal");
        await Expect(Page.Locator("#mealModal")).ToBeVisibleAsync();
        await AssertModalFitsViewportAsync("mealModal");
    }

    [Test]
    public async Task PhonePlanner_KeepsPrimaryInteractiveElementsUsable()
    {
        await AssertAllMinimumSizeAsync(".quick-action", 240, 56);
        await AssertAllMinimumSizeAsync(".shop-check", 36, 36);

        await Page.GetByRole(AriaRole.Button, new() { Name = "Registrer utgift", Exact = true }).ClickAsync();
        await WaitForModalStateAsync("budgetModal", open: true);
        await AssertModalFitsViewportAsync("budgetModal");
        await AssertAllMinimumSizeAsync("#budgetModal .tabs button", 44, 44);
        await Page.GetByRole(AriaRole.Button, new() { Name = "Avbryt", Exact = true }).First.ClickAsync();
        await WaitForModalStateAsync("budgetModal", open: false);

        await Page.Locator(".family-avatar").First.ClickAsync();
        await WaitForModalStateAsync("profileModal", open: true);
        await AssertModalFitsViewportAsync("profileModal");
        await AssertMinimumSizeAsync(".profile-close", 44, 44);
        await Page.Locator(".profile-close").ClickAsync();
        await WaitForModalStateAsync("profileModal", open: false);

        await DragFamilyMemberToDayAsync("Anna", 0);
        await WaitForModalStateAsync("eventModal", open: true);
        await AssertModalFitsViewportAsync("eventModal");
        await Expect(Page.Locator("#eventOwner")).Not.ToHaveValueAsync("");
    }

    [Test]
    public async Task PhonePlanner_ConstrainsStackedCalendarDayCardsWithLongEvents()
    {
        var today = DateOnly.FromDateTime(DateTime.Today).ToString("yyyy-MM-dd");
        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Morgenlogistikk og matpakker med ekstra lang tekst",
            ["event_date"] = today,
            ["start_time"] = "06:45",
            ["end_time"] = "07:20",
            ["note"] = "Lang beskrivelse som skal vises som Vis mer og ikke presse dagkortet bredere.",
        });

        await Page.ReloadAsync();
        await Expect(Page.Locator(".quick-actions")).ToBeVisibleAsync();
        await Expect(Page.Locator($".day-box[data-date='{today}'] .event-item", new() { HasTextString = "Morgenlogistikk" }).First).ToBeVisibleAsync();
        await AssertNoHorizontalOverflowAsync();
        await AssertLocatorFitsViewportWidthAsync(".calendar-wrapper");

        var layout = await Page.Locator($".day-box[data-date='{today}']").EvaluateAsync<double[]>(
            @"day => {
                const wrapper = document.querySelector('.calendar-wrapper');
                const header = day.querySelector('.day-header');
                const content = day.querySelector('.day-content');
                const items = Array.from(day.querySelectorAll('.event-item'));
                const rect = element => {
                    const value = element.getBoundingClientRect();
                    return [value.left, value.right, value.width];
                };
                const itemLeft = Math.min(...items.map(item => item.getBoundingClientRect().left));
                const itemRight = Math.max(...items.map(item => item.getBoundingClientRect().right));
                return [
                    ...rect(wrapper),
                    ...rect(day),
                    ...rect(header),
                    ...rect(content),
                    itemLeft,
                    itemRight,
                    document.documentElement.scrollWidth,
                    window.innerWidth
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(layout[3], Is.GreaterThanOrEqualTo(layout[0] - 1d), "Stacked day card should not protrude left of the calendar surface.");
            Assert.That(layout[4], Is.LessThanOrEqualTo(layout[1] + 1d), "Stacked day card should not protrude right of the calendar surface.");
            Assert.That(layout[6], Is.GreaterThanOrEqualTo(layout[3] - 1d), "Stacked day header should stay inside the day card.");
            Assert.That(layout[7], Is.LessThanOrEqualTo(layout[4] + 1d), "Stacked day header should stay inside the day card.");
            Assert.That(layout[9], Is.GreaterThanOrEqualTo(layout[3] - 1d), "Stacked day content should stay inside the day card.");
            Assert.That(layout[10], Is.LessThanOrEqualTo(layout[4] + 1d), "Stacked day content should stay inside the day card.");
            Assert.That(layout[12], Is.GreaterThanOrEqualTo(layout[3] - 1d), "Stacked event cards should stay inside the day card.");
            Assert.That(layout[13], Is.LessThanOrEqualTo(layout[4] + 1d), "Stacked event cards should stay inside the day card.");
            Assert.That(layout[14], Is.LessThanOrEqualTo(layout[15] + 1d), "Stacked calendar should not create page-level horizontal overflow.");
        });
    }
}
