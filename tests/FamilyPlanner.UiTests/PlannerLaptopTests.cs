using Microsoft.Playwright;
using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class PlannerLaptopTests : PlannerUiTestBase
{
    protected override int ViewportWidth => 1280;
    protected override int ViewportHeight => 900;

    [Test]
    public async Task LaptopPlanner_UsesKioskLayoutAboveThreshold()
    {
        Assert.That(await GetGridColumnCountAsync(".main-container"), Is.EqualTo(3));
        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(6));
        Assert.That(await GetGridColumnCountAsync(".week-days"), Is.EqualTo(7));
        Assert.That(await GetGridColumnCountAsync(".meals-grid"), Is.EqualTo(7));

        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");
        await AssertLocatorFitsViewportWidthAsync(".main-container");
        await AssertNoHorizontalOverflowAsync();
        await AssertAllMinimumSizeAsync(".week-nav-btn", 44, 44);

        await Expect(Page.Locator(".budget-card")).ToBeVisibleAsync();
        await Expect(Page.Locator(".planner-surface")).ToBeVisibleAsync();
        await Expect(Page.Locator(".upcoming-card")).ToBeVisibleAsync();
    }
}
