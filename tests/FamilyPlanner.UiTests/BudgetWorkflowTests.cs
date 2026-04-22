using NUnit.Framework;
using System.Text.RegularExpressions;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class BudgetWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task BudgetAndExpenses_UpdateAndPersistThroughUi()
    {
        await OpenModalBySelectorAsync(".quick-action:has-text('Utgift')", "budgetModal");
        await Page.Locator("#budgetModal .tabs button").Nth(1).ClickAsync();
        await Page.Locator("#budgetIncomeInput").FillAsync("70000");
        await Page.Locator("#budgetLimitInput").FillAsync("22000");
        await Page.Locator("#budgetForm .btn-primary").ClickAsync();
        await WaitForModalStateAsync("budgetModal", open: false);

        var updatedBudget = await GetApiAsync<BudgetSnapshotDto>("/api/budget")
            ?? throw new AssertionException("Budget snapshot was null.");
        Assert.That(updatedBudget.Income, Is.EqualTo(70000));
        Assert.That(updatedBudget.Limit, Is.EqualTo(22000));

        await OpenModalBySelectorAsync(".quick-action:has-text('Utgift')", "budgetModal");
        await Assert.ThatAsync(() => Page.Locator("#expenseForm").IsVisibleAsync(), Is.True);
        await Assert.ThatAsync(() => Page.Locator("#budgetForm").IsVisibleAsync(), Is.False);
        await Page.Locator("#budgetModal .tabs button").Nth(0).ClickAsync();
        await Page.Locator("#expenseAmount").FillAsync("599");
        await Page.Locator("#expenseCategory").FillAsync("Leisure");
        await Page.Locator("#expenseDesc").FillAsync("Movie night");
        await Page.Locator("#expenseForm .btn-primary").ClickAsync();
        await WaitForModalStateAsync("budgetModal", open: false);

        var withExpense = await GetApiAsync<BudgetSnapshotDto>("/api/budget")
            ?? throw new AssertionException("Budget snapshot after expense was null.");
        var createdExpense = withExpense.Expenses.Single(x => x.Description == "Movie night");
        Assert.That(withExpense.Spent, Is.GreaterThan(updatedBudget.Spent));

        await OpenModalBySelectorAsync(".quick-action:has-text('Utgift')", "budgetModal");
        await Page.Locator("#budgetModal .tabs button").Nth(2).ClickAsync();
        await Page.Locator("#expenseHistory").WaitForAsync();
        var expenseDelete = Page.Locator("#expenseList .shop-item", new() { HasTextString = "Movie night" }).Locator("button", new() { HasTextString = "Slett" });
        await Expect(expenseDelete).ToBeVisibleAsync();
        await Expect(expenseDelete).ToHaveClassAsync(new Regex("btn-danger"));
        var deleteStyle = await expenseDelete.EvaluateAsync<string[]>(
            "button => [getComputedStyle(button).backgroundImage, getComputedStyle(button).color]");
        Assert.Multiple(() =>
        {
            Assert.That(deleteStyle[0], Does.Contain("gradient"), "Budget history delete should use the global danger gradient.");
            Assert.That(deleteStyle[1], Is.EqualTo("rgb(255, 255, 255)"), "Budget history delete text should be legible.");
        });
        await expenseDelete.ClickAsync();
        await Page.Locator("#expenseList .shop-item", new() { HasTextString = "Movie night" }).WaitForAsync(new() { State = Microsoft.Playwright.WaitForSelectorState.Detached });

        var finalBudget = await GetApiAsync<BudgetSnapshotDto>("/api/budget")
            ?? throw new AssertionException("Final budget snapshot was null.");
        Assert.That(finalBudget.Expenses.Any(x => x.Id == createdExpense.Id), Is.False);
    }
}
