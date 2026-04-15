using Microsoft.Playwright;
using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class PlannerDesktopTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task DesktopPlanner_UsesThreeColumnWorkspaceAndWeekNavigation()
    {
        Assert.That(await GetGridColumnCountAsync(".main-container"), Is.EqualTo(3));
        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(6));

        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");
        await AssertLocatorFitsViewportWidthAsync(".main-container");
        await AssertNoHorizontalOverflowAsync();
        await AssertAllMinimumSizeAsync(".week-nav-btn", 44, 44);

        var originalRange = (await Page.Locator("#weekRange").InnerTextAsync()).Trim();
        await Page.GetByRole(AriaRole.Button, new() { Name = "Neste" }).ClickAsync();
        await WaitForTextChangeAsync("#weekRange", originalRange);
        await Page.GetByRole(AriaRole.Button, new() { Name = "Denne uken" }).ClickAsync();
        await WaitForExactTextAsync("#weekRange", originalRange);

        await Expect(Page.Locator(".planner-toolbar")).ToBeVisibleAsync();
        await Expect(Page.Locator(".shopping-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".upcoming-card")).ToBeVisibleAsync();
    }
}
