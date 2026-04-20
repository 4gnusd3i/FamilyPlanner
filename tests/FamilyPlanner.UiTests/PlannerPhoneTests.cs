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
        Assert.That(await Page.Locator(".add-day-btn").CountAsync(), Is.EqualTo(0), "Per-day add appointment buttons should be deprecated.");

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
        await WaitForModalStateAsync("assignModal", open: true);
        await AssertModalFitsViewportAsync("assignModal");
        await AssertAllMinimumSizeAsync(".act-btn", 44, 44);
    }
}
