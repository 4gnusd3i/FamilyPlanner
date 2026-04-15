using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class MealWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task MealCrud_AndMealToShoppingShortcut_PersistCorrectly()
    {
        var family = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var annaId = family.Single(x => x.Name == "Anna").Id;

        await OpenModalBySelectorAsync(".add-meal-btn", "mealModal");
        await Page.Locator("#mealDay").SelectOptionAsync("2");
        await Page.Locator("#mealType").SelectOptionAsync("breakfast");
        await Page.Locator("#mealName").FillAsync("Pancakes");
        await Page.Locator("#mealOwner").SelectOptionAsync(annaId.ToString());
        await Page.Locator("#mealNote").FillAsync("Oats and milk");
        await Page.Locator("#mealModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("mealModal", open: false);

        await Expect(Page.Locator(".meal-entry", new() { HasTextString = "Pancakes" })).ToBeVisibleAsync();

        var createdMeals = await GetApiAsync<List<MealPlanDto>>("/api/meals") ?? [];
        var createdMeal = createdMeals.Single(x => x.Meal == "Pancakes");

        await Page.Locator(".meal-entry", new() { HasTextString = "Pancakes" }).ClickAsync();
        await WaitForModalStateAsync("mealModal", open: true);
        await Page.Locator("#mealName").FillAsync("Wholegrain pancakes");
        await Page.Locator("#mealNote").FillAsync("Oats, milk and berries");
        await Page.Locator("#mealModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("mealModal", open: false);

        var updatedMeals = await GetApiAsync<List<MealPlanDto>>("/api/meals") ?? [];
        var updatedMeal = updatedMeals.Single(x => x.Id == createdMeal.Id);
        Assert.That(updatedMeal.Meal, Is.EqualTo("Wholegrain pancakes"));
        Assert.That(updatedMeal.Note, Is.EqualTo("Oats, milk and berries"));

        var mealCard = Page.Locator(".meal-entry", new() { HasTextString = "Wholegrain pancakes" });
        await AcceptDialogAsync(
            () => mealCard.Locator(".meal-add-btn").ClickAsync(),
            "Legge");
        await WaitForToastAsync("Handlelisten er oppdatert.");

        var shoppingItems = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        Assert.That(shoppingItems.Any(x => x.Item == "Oats, milk and berries"), Is.True);

        await mealCard.ClickAsync();
        await WaitForModalStateAsync("mealModal", open: true);
        await AcceptDialogAsync(
            () => Page.Locator("#deleteMealBtn").ClickAsync(),
            "Slette");

        await Expect(Page.Locator(".meal-entry", new() { HasTextString = "Wholegrain pancakes" })).ToHaveCountAsync(0);
        var finalMeals = await GetApiAsync<List<MealPlanDto>>("/api/meals") ?? [];
        Assert.That(finalMeals.Any(x => x.Id == createdMeal.Id), Is.False);
    }
}
