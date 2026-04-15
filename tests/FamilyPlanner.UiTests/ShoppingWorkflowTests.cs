using NUnit.Framework;
using System.Text.RegularExpressions;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class ShoppingWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task ShoppingCrud_AndToggle_PersistCorrectly()
    {
        await OpenModalBySelectorAsync(".quick-action:has-text('Vare')", "shoppingModal");
        await Page.Locator("#shoppingItem").FillAsync("Diapers");
        await Page.Locator("#shoppingQty").FillAsync("3");
        await Page.Locator("#shoppingModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("shoppingModal", open: false);

        var createdItems = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        var createdItem = createdItems.Single(x => x.Item == "Diapers");
        Assert.That(createdItem.Quantity, Is.EqualTo(3));
        Assert.That(createdItems.Count, Is.EqualTo(2));
        Assert.That(createdItems.Count(x => !x.Done), Is.EqualTo(2));

        var shoppingRow = Page.Locator(".shop-item", new() { HasTextString = "Diapers" });
        await shoppingRow.Locator(".shop-check").ClickAsync();
        await Expect(shoppingRow.Locator(".shop-check")).ToHaveClassAsync(new Regex("checked"));
        await Expect(shoppingRow.Locator(".shop-check")).ToHaveClassAsync(new Regex("taken"));
        await Expect(shoppingRow).ToHaveClassAsync(new Regex("is-delete-pending"));

        var toggledItems = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        var toggledItem = toggledItems.Single(x => x.Id == createdItem.Id);
        Assert.That(toggledItem.Done, Is.True);
        Assert.That(toggledItem.DoneAt, Is.Not.Null);
        Assert.That(toggledItems.Count, Is.EqualTo(2));
        Assert.That(toggledItems.Count(x => !x.Done), Is.EqualTo(1));

        await shoppingRow.Locator(".shop-check").ClickAsync();
        await Page.WaitForFunctionAsync(
            @"itemText => {
                const row = Array.from(document.querySelectorAll('.shop-item'))
                    .find((element) => element.textContent.includes(itemText));
                return !!row && !row.querySelector('.shop-check')?.classList.contains('checked');
            }",
            "Diapers");
        var canceledItems = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        var canceledItem = canceledItems.Single(x => x.Id == createdItem.Id);
        Assert.That(canceledItem.Done, Is.False);
        Assert.That(canceledItem.DoneAt, Is.Null);

        await shoppingRow.Locator(".shop-name").ClickAsync();
        await WaitForModalStateAsync("shoppingModal", open: true);
        await Expect(Page.Locator("#deleteShoppingBtn")).ToHaveCountAsync(0);
        await Page.Locator("#shoppingItem").FillAsync("Night diapers");
        await Page.Locator("#shoppingQty").FillAsync("5");
        await Page.Locator("#shoppingModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("shoppingModal", open: false);

        var updatedItems = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        var updatedItem = updatedItems.Single(x => x.Id == createdItem.Id);
        Assert.That(updatedItem.Item, Is.EqualTo("Night diapers"));
        Assert.That(updatedItem.Quantity, Is.EqualTo(5));

        await OpenModalBySelectorAsync(".quick-action:has-text('Vare')", "shoppingModal");
        await Page.Locator("#shoppingItem").FillAsync("Wipes");
        await Page.Locator("#shoppingQty").FillAsync("2");
        await Page.Locator("#shoppingModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("shoppingModal", open: false);

        var withExtraItem = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        Assert.That(withExtraItem.Any(x => x.Item == "Night diapers"), Is.True, "Existing shopping item should remain after adding a new item.");
        Assert.That(withExtraItem.Any(x => x.Item == "Wipes"), Is.True, "Newly added shopping item should be appended, not replace an existing item.");
        Assert.That(withExtraItem.Count, Is.EqualTo(3), "Adding a new shopping item should increase total list size.");

        var nightDiapersRow = Page.Locator(".shop-item", new() { HasTextString = "Night diapers" });
        await nightDiapersRow.Locator(".shop-check").ClickAsync();
        await Expect(nightDiapersRow).ToHaveClassAsync(new Regex("is-delete-pending"));
        await Expect(nightDiapersRow).ToHaveCountAsync(0, new() { Timeout = 18_000 });

        var finalItems = await GetApiAsync<List<ShoppingItemDto>>("/api/shopping") ?? [];
        Assert.That(finalItems.Any(x => x.Id == createdItem.Id), Is.False);
        Assert.That(finalItems.Count, Is.EqualTo(2));
        Assert.That(finalItems.Count(x => !x.Done), Is.EqualTo(2));
    }
}
