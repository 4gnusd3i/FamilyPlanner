using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture(1366, 768)]
[TestFixture(1920, 1080)]
[TestFixture(2560, 1440)]
public sealed class PlannerDesktopViewportTests : PlannerUiTestBase
{
    private readonly int _viewportWidth;
    private readonly int _viewportHeight;

    public PlannerDesktopViewportTests(int viewportWidth, int viewportHeight)
    {
        _viewportWidth = viewportWidth;
        _viewportHeight = viewportHeight;
    }

    protected override int ViewportWidth => _viewportWidth;
    protected override int ViewportHeight => _viewportHeight;

    [Test]
    public async Task DesktopViewport_KioskRegionsStaySeparatedAndWithinViewport()
    {
        await Expect(Page.Locator(".quick-actions")).ToBeVisibleAsync();
        await Expect(Page.Locator(".main-container")).ToBeVisibleAsync();
        await Expect(Page.Locator(".family-bar")).ToBeVisibleAsync();

        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(5));
        Assert.That(await GetGridColumnCountAsync(".main-container"), Is.EqualTo(3));
        Assert.That(await GetGridColumnCountAsync(".week-days"), Is.EqualTo(7));
        Assert.That(await GetGridColumnCountAsync(".meals-grid"), Is.EqualTo(7));
        await Expect(Page.Locator(".meal-day-header")).ToHaveCountAsync(0);

        await AssertNoHorizontalOverflowAsync();
        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".main-container");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");
        await AssertAllMinimumSizeAsync(".quick-action", 44, 44);
        await AssertAllMinimumSizeAsync(".week-nav-btn", 44, 44);

        var layout = await Page.EvaluateAsync<double[]>(
            @"() => {
                const rect = (selector) => {
                    const element = document.querySelector(selector);
                    if (!element) throw new Error(`Missing ${selector}`);
                    const box = element.getBoundingClientRect();
                    return { left: box.left, top: box.top, right: box.right, bottom: box.bottom, width: box.width, height: box.height };
                };
                const actions = rect('.quick-actions');
                const workspace = rect('.main-container');
                const family = rect('.family-bar');
                const planner = rect('.planner-surface');
                const meals = rect('.meals-surface');
                const notes = rect('.notes-card');
                const upcoming = rect('.upcoming-card');
                const quickStyles = getComputedStyle(document.querySelector('.quick-actions'));
                const familyStyles = getComputedStyle(document.querySelector('.family-bar'));
                const actionHeights = Array.from(document.querySelectorAll('.quick-action'))
                    .map((item) => item.getBoundingClientRect().height);
                const familyShellHeights = Array.from(document.querySelectorAll('.family-avatar-shell'))
                    .map((item) => item.getBoundingClientRect().height);
                return [
                    document.documentElement.scrollWidth,
                    window.innerWidth,
                    document.documentElement.scrollHeight,
                    window.innerHeight,
                    actions.bottom,
                    workspace.top,
                    workspace.bottom,
                    family.top,
                    family.bottom,
                    planner.bottom,
                    meals.top,
                    meals.bottom,
                    notes.bottom,
                    upcoming.bottom,
                    quickStyles.boxShadow === 'none' ? 1 : 0,
                    familyStyles.boxShadow === 'none' ? 1 : 0,
                    familyStyles.backgroundColor === 'rgba(0, 0, 0, 0)' ? 1 : 0,
                    Math.max(...actionHeights),
                    Math.max(...familyShellHeights)
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(layout[0], Is.LessThanOrEqualTo(layout[1] + 1d), "Desktop viewport should not have horizontal page overflow.");
            Assert.That(layout[2], Is.LessThanOrEqualTo(layout[3] + 2d), "Desktop kiosk layout should fit without page-level vertical scrolling.");
            Assert.That(layout[4], Is.LessThanOrEqualTo(layout[5] + 1d), "Quick actions should not overlap the workspace.");
            Assert.That(layout[6], Is.LessThanOrEqualTo(layout[7] + 1d), "Workspace should not overlap the family row.");
            Assert.That(layout[8], Is.LessThanOrEqualTo(layout[3] + 2d), "Family row should stay within the viewport.");
            Assert.That(layout[9], Is.LessThanOrEqualTo(layout[10] + 1d), "Calendar should not overlap the meals row.");
            Assert.That(layout[11], Is.LessThanOrEqualTo(layout[7] + 1d), "Meals row should not overlap the family row.");
            Assert.That(layout[12], Is.LessThanOrEqualTo(layout[7] + 1d), "Notes should not overlap the family row.");
            Assert.That(layout[13], Is.LessThanOrEqualTo(layout[7] + 1d), "Upcoming should not overlap the family row.");
            Assert.That(layout[14], Is.EqualTo(1d), "Quick actions should stay visually lightweight in kiosk mode.");
            Assert.That(layout[15], Is.EqualTo(1d), "Family row should not regain a card shadow in kiosk mode.");
            Assert.That(layout[16], Is.EqualTo(1d), "Family row should stay transparent in kiosk mode.");
            Assert.That(layout[17], Is.LessThanOrEqualTo(56d), "Quick actions should stay compact on desktop kiosk viewports.");
            Assert.That(layout[18], Is.LessThanOrEqualTo(52d), "Avatar row should stay compact on desktop kiosk viewports.");
        });
    }

    [Test]
    public async Task DesktopViewport_DenseContentUsesInternalScrollInsteadOfBreakingLayout()
    {
        await AddDenseViewportContentAsync();
        await Page.ReloadAsync();

        var today = DateTime.Today.ToString("yyyy-MM-dd");
        var daySelector = $".day-box[data-date='{today}']";

        await Expect(Page.Locator(".quick-actions")).ToBeVisibleAsync();
        await Expect(Page.Locator(daySelector)).ToBeVisibleAsync();
        await Expect(Page.Locator(".note-item", new() { HasTextString = "Langt notat 4" })).ToBeVisibleAsync();
        await Expect(Page.Locator(".shop-item", new() { HasTextString = "Ekstra vare 7" })).ToBeVisibleAsync();
        await AssertNoHorizontalOverflowAsync();
        await Expect(Page.Locator(".meal-day-header")).ToHaveCountAsync(0);

        var layout = await Page.EvaluateAsync<double[]>(
            @"([dateSelector]) => {
                const rect = (element) => {
                    const box = element.getBoundingClientRect();
                    return { left: box.left, top: box.top, right: box.right, bottom: box.bottom, width: box.width, height: box.height };
                };
                const workspace = rect(document.querySelector('.main-container'));
                const family = rect(document.querySelector('.family-bar'));
                const notes = rect(document.querySelector('.notes-card'));
                const shopping = rect(document.querySelector('.shopping-card'));
                const meals = rect(document.querySelector('.meals-surface'));
                const notesList = document.querySelector('#notesList');
                const shoppingList = document.querySelector('#shoppingList');
                const notesListStyles = getComputedStyle(notesList);
                const shoppingListStyles = getComputedStyle(shoppingList);
                const day = document.querySelector(dateSelector);
                const dayRect = rect(day);
                const dayContent = day.querySelector('.day-content');
                const longEvent = Array.from(dayContent.querySelectorAll('.event-item'))
                    .find((item) => item.textContent.includes('Ekstra lang avtaletittel'));
                const title = longEvent.querySelector('.event-title');
                const description = longEvent.querySelector('.event-description');
                const allEventsFitDay = Array.from(dayContent.querySelectorAll('.event-item')).every((item) => {
                    const itemRect = rect(item);
                    return itemRect.left >= dayRect.left - 1 &&
                        itemRect.right <= dayRect.right + 1 &&
                        itemRect.top >= dayRect.top - 1;
                });
                const mealsRect = meals;
                const mealsFit = Array.from(document.querySelectorAll('.meal-entry')).every((item) => {
                    const itemRect = rect(item);
                    return itemRect.left >= mealsRect.left - 1 &&
                        itemRect.right <= mealsRect.right + 1 &&
                        itemRect.top >= mealsRect.top - 1 &&
                        itemRect.bottom <= mealsRect.bottom + 1;
                });
                return [
                    document.documentElement.scrollHeight,
                    window.innerHeight,
                    workspace.bottom,
                    family.top,
                    notes.bottom,
                    shopping.bottom,
                    notesList.scrollHeight - notesList.clientHeight,
                    shoppingList.scrollHeight - shoppingList.clientHeight,
                    notesListStyles.overflowY === 'auto' ? 1 : 0,
                    shoppingListStyles.overflowY === 'auto' ? 1 : 0,
                    dayContent.scrollHeight > dayContent.clientHeight ? 1 : 0,
                    allEventsFitDay ? 1 : 0,
                    title.scrollWidth > title.clientWidth ? 1 : 0,
                    description.scrollHeight > description.clientHeight ? 1 : 0,
                    mealsFit ? 1 : 0,
                    meals.bottom,
                    family.top
                ];
            }",
            new object[] { daySelector });

        Assert.Multiple(() =>
        {
            Assert.That(layout[0], Is.LessThanOrEqualTo(layout[1] + 2d), "Dense desktop content should not create page-level vertical scrolling.");
            Assert.That(layout[2], Is.LessThanOrEqualTo(layout[3] + 1d), "Dense workspace content should stay above the family row.");
            Assert.That(layout[4], Is.LessThanOrEqualTo(layout[3] + 1d), "Dense notes should scroll internally before reaching the family row.");
            Assert.That(layout[5], Is.LessThanOrEqualTo(layout[3] + 1d), "Dense shopping list should scroll internally before reaching the family row.");
            Assert.That(layout[6], Is.GreaterThanOrEqualTo(-1d), "Dense notes should either fit or overflow only inside the notes list.");
            Assert.That(layout[7], Is.GreaterThanOrEqualTo(-1d), "Dense shopping should either fit or overflow only inside the shopping list.");
            Assert.That(layout[8], Is.EqualTo(1d), "Notes list should keep internal scrolling available when desktop content exceeds available height.");
            Assert.That(layout[9], Is.EqualTo(1d), "Shopping list should keep internal scrolling available when desktop content exceeds available height.");
            Assert.That(layout[10], Is.EqualTo(1d), "Dense calendar days should use internal scrolling.");
            Assert.That(layout[11], Is.EqualTo(1d), "Calendar event cards should stay within their day column.");
            Assert.That(layout[12], Is.EqualTo(1d), "Long desktop event titles should ellipsize inside the card.");
            Assert.That(layout[13], Is.EqualTo(1d), "Long desktop event descriptions should clamp inside the card.");
            Assert.That(layout[14], Is.EqualTo(1d), "Meal cards should stay contained in the meal row.");
            Assert.That(layout[15], Is.LessThanOrEqualTo(layout[16] + 1d), "Meal row should stay above the family row.");
        });
    }

    private async Task AddDenseViewportContentAsync()
    {
        var family = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var annaId = family.Single(x => x.Name == "Anna").Id;
        var today = DateTime.Today.ToString("yyyy-MM-dd");

        await PostFormAsync("/api/events", new Dictionary<string, string>
        {
            ["title"] = "Ekstra lang avtaletittel som skal kuttes rent uten aa stikke ut av kalenderdagen",
            ["event_date"] = today,
            ["start_time"] = "17:10",
            ["end_time"] = "18:40",
            ["owner_id"] = annaId.ToString(),
            ["note"] = "Dette er en lang beskrivelse som skal holde seg inne i kortet, klampes rent og ikke dytte resten av oppsettet ut av kurs.",
        });

        for (var index = 1; index <= 8; index += 1)
        {
            await PostFormAsync("/api/events", new Dictionary<string, string>
            {
                ["title"] = $"Tett skrivebordsavtale {index}",
                ["event_date"] = today,
                ["start_time"] = $"1{index % 10}:00",
                ["end_time"] = $"1{index % 10}:30",
                ["owner_id"] = annaId.ToString(),
                ["note"] = $"Kort notat {index}",
            });

            await PostFormAsync("/api/notes", new Dictionary<string, string>
            {
                ["title"] = $"Langt notat {index}",
                ["owner_id"] = annaId.ToString(),
                ["content"] = $"Innhold {index}",
            });

            await PostFormAsync("/api/shopping", new Dictionary<string, string>
            {
                ["item"] = $"Ekstra vare {index}",
                ["quantity"] = "1",
                ["owner_id"] = annaId.ToString(),
            });
        }
    }
}
