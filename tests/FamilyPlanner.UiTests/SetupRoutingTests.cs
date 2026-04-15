using System.Text.RegularExpressions;
using Microsoft.Playwright;
using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class SetupRoutingTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task SetupPage_RedirectsToPlannerAfterInitialization()
    {
        await Page.GotoAsync("/setup");
        await Expect(Page).ToHaveURLAsync(new Regex(".*/$"));
        await Expect(Page.Locator(".quick-actions")).ToBeVisibleAsync();
    }
}
