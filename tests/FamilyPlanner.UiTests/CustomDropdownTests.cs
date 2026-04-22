using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class CustomDropdownTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task ModalCustomDropdowns_AreStyledCappedKeyboardSelectableAndSubmitValues()
    {
        for (var index = 1; index <= 18; index += 1)
        {
            await PostFormAsync("/api/family", new Dictionary<string, string>
            {
                ["name"] = $"Dropdown medlem {index}",
                ["color"] = "#2563eb",
            });
        }

        await Page.ReloadAsync();
        var family = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var anna = family.Single(x => x.Name == "Anna");

        await OpenModalBySelectorAsync(".quick-action:has-text('Avtale')", "eventModal");

        var customSelectIds = await Page.Locator(".custom-select").EvaluateAllAsync<string[]>(
            "items => items.map(item => item.dataset.selectId).sort()");
        Assert.That(customSelectIds, Is.EquivalentTo(new[]
        {
            "eventOwner",
            "eventRecurrenceType",
            "expenseOwner",
            "mealDay",
            "mealOwner",
            "mealType",
            "noteOwner",
            "shoppingOwner",
        }));

        var recurrenceRoot = Page.Locator(".custom-select[data-select-id='eventRecurrenceType']");
        await recurrenceRoot.Locator(".custom-select-trigger").ClickAsync();
        await Page.Keyboard.PressAsync("ArrowDown");
        await Page.Keyboard.PressAsync("Enter");
        await Expect(Page.Locator("#eventRecurrenceType")).ToHaveValueAsync("daily");
        await Expect(Page.Locator("#eventRecurrenceUntil")).ToBeVisibleAsync();

        var ownerRoot = Page.Locator(".custom-select[data-select-id='eventOwner']");
        await ownerRoot.Locator(".custom-select-trigger").ClickAsync();
        var ownerMenuMetrics = await ownerRoot.EvaluateAsync<double[]>(
            @"root => {
                const trigger = root.querySelector('.custom-select-trigger').getBoundingClientRect();
                const menu = root.querySelector('.custom-select-menu');
                const menuRect = menu.getBoundingClientRect();
                const styles = getComputedStyle(menu);
                return [
                    Math.abs(trigger.width - menuRect.width),
                    menu.clientHeight,
                    menu.scrollHeight,
                    styles.overflowY === 'auto' ? 1 : 0,
                    menuRect.left,
                    window.innerWidth - menuRect.right,
                    menuRect.top,
                    window.innerHeight - menuRect.bottom
                ];
            }");

        Assert.Multiple(() =>
        {
            Assert.That(ownerMenuMetrics[0], Is.LessThanOrEqualTo(1d), "Custom dropdown menu should stay trigger-width.");
            Assert.That(ownerMenuMetrics[1], Is.LessThanOrEqualTo(224d), "Custom dropdown menu should cap vertical height.");
            Assert.That(ownerMenuMetrics[2], Is.GreaterThan(ownerMenuMetrics[1]), "Large dropdown menus should scroll internally.");
            Assert.That(ownerMenuMetrics[3], Is.EqualTo(1d), "Custom dropdown menu should use internal vertical scrolling.");
            Assert.That(ownerMenuMetrics[4], Is.GreaterThanOrEqualTo(0d), "Custom dropdown should not overflow the left viewport edge.");
            Assert.That(ownerMenuMetrics[5], Is.GreaterThanOrEqualTo(0d), "Custom dropdown should not overflow the right viewport edge.");
            Assert.That(ownerMenuMetrics[6], Is.GreaterThanOrEqualTo(0d), "Custom dropdown should not overflow the top viewport edge.");
            Assert.That(ownerMenuMetrics[7], Is.GreaterThanOrEqualTo(0d), "Custom dropdown should not overflow the bottom viewport edge.");
        });

        await ownerRoot.Locator($".custom-select-option[data-value='{anna.Id}']").ClickAsync();
        await Expect(Page.Locator("#eventOwner")).ToHaveValueAsync(anna.Id.ToString());

        await Page.Locator("#eventTitle").FillAsync("Dropdown proof");
        await Page.Locator("#eventDate").FillAsync(DateTime.Today.ToString("yyyy-MM-dd"));
        await Page.Locator("#eventRecurrenceUntil").FillAsync(DateTime.Today.AddDays(1).ToString("yyyy-MM-dd"));
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        var events = await GetApiAsync<List<PlannerEventDto>>(
            $"/api/events?start={DateTime.Today:yyyy-MM-dd}&end={DateTime.Today.AddDays(1):yyyy-MM-dd}") ?? [];
        var createdEvent = events.First(x => x.Title == "Dropdown proof");
        Assert.Multiple(() =>
        {
            Assert.That(createdEvent.OwnerId, Is.EqualTo(anna.Id));
            Assert.That(createdEvent.RecurrenceType, Is.EqualTo("daily"));
        });
    }
}
