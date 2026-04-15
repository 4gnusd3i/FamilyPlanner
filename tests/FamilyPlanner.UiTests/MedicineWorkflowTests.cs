using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class MedicineWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task MedicineCrud_ViewAndToggle_PersistCorrectly()
    {
        await OpenModalBySelectorAsync(".quick-action:has-text('Medisin')", "medicineModal");

        var defaultTime = await Page.EvaluateAsync<int[]>(
            @"() => {
                const [h, m] = document.querySelector('#medicineTime').value.split(':').map(Number);
                const now = new Date();
                return [((h * 60) + m), ((now.getHours() * 60) + now.getMinutes())];
            }");
        Assert.That(defaultTime[0], Is.InRange(defaultTime[1] - 2, defaultTime[1] + 2), "Medicine time should default to current local time.");

        await Page.Locator("#medicineName").FillAsync("Vitamin D");
        await Page.Locator("#medicineTime").FillAsync("08:15");
        await Page.Locator("#medicineNote").FillAsync("Morning dose");
        await Page.Locator("#medicineModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("medicineModal", open: false);

        var createdMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        var createdMedicine = createdMedicines.Single(x => x.Name == "Vitamin D");
        Assert.That(createdMedicine.Taken, Is.False);

        var medicineRow = Page.Locator(".med-item", new() { HasTextString = "Vitamin D" });
        await medicineRow.ClickAsync();
        await WaitForModalStateAsync("medicineViewModal", open: true);
        await Page.Locator("#medicineViewModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("medicineModal", open: true);
        await Page.Locator("#medicineNote").FillAsync("After breakfast");
        await Page.Locator("#medicineModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("medicineModal", open: false);

        var updatedMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        Assert.That(updatedMedicines.Single(x => x.Id == createdMedicine.Id).Note, Is.EqualTo("After breakfast"));

        await Page.Locator(".med-item", new() { HasTextString = "Vitamin D" }).ClickAsync();
        await WaitForModalStateAsync("medicineViewModal", open: true);
        await AcceptDialogAsync(
            () => Page.Locator("#medicineViewModal .btn-danger").ClickAsync(),
            "Slette");

        var finalMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        Assert.That(finalMedicines.Any(x => x.Id == createdMedicine.Id), Is.False);
    }

    [Test]
    public async Task CheckedPastMedicine_IsDeletedOnNextRead()
    {
        var pastTime = DateTime.Now.TimeOfDay > TimeSpan.FromSeconds(2)
            ? DateTime.Now.AddSeconds(-2).ToString("HH:mm:ss")
            : "00:00:00";

        await PostFormAsync("/api/medicines", new Dictionary<string, string>
        {
            ["name"] = "Expired dose",
            ["time"] = pastTime,
        });

        var createdMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        var createdMedicine = createdMedicines.Single(x => x.Name == "Expired dose");

        await PostJsonAsync("/api/medicines", new { toggle = true, id = createdMedicine.Id });

        var remainingMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        Assert.That(remainingMedicines.Any(x => x.Id == createdMedicine.Id), Is.False);
    }
}
