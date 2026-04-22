using NUnit.Framework;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class PlannerStackedViewportTests : PlannerUiTestBase
{
    protected override int ViewportWidth => 999;
    protected override int ViewportHeight => 768;

    [Test]
    public async Task BelowKioskBreakpoint_UsesSingleColumnWithExpectedVerticalScroll()
    {
        Assert.That(await GetGridColumnCountAsync(".quick-action-grid"), Is.EqualTo(1));
        Assert.That(await GetGridColumnCountAsync(".main-container"), Is.EqualTo(1));
        Assert.That(await GetGridColumnCountAsync(".week-days"), Is.EqualTo(1));
        Assert.That(await GetGridColumnCountAsync(".meals-grid"), Is.EqualTo(1));

        await AssertNoHorizontalOverflowAsync();
        await AssertLocatorFitsViewportWidthAsync(".quick-actions");
        await AssertLocatorFitsViewportWidthAsync(".main-container");
        await AssertLocatorFitsViewportWidthAsync(".family-bar");

        var pageScroll = await Page.EvaluateAsync<double[]>(
            @"() => [
                document.documentElement.scrollHeight,
                window.innerHeight,
                getComputedStyle(document.querySelector('.app-shell')).position === 'fixed' ? 1 : 0
            ]");

        Assert.Multiple(() =>
        {
            Assert.That(pageScroll[0], Is.GreaterThan(pageScroll[1]), "Stacked mode should use normal vertical page scrolling.");
            Assert.That(pageScroll[2], Is.EqualTo(0d), "Stacked mode should not keep the fixed kiosk canvas.");
        });
    }
}
