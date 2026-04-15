using NUnit.Framework;
using System.Text.RegularExpressions;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class MedicineWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task MedicineCrud_ViewAndToggle_PersistCorrectly()
    {
        await OpenModalBySelectorAsync(".medicine-card .inline-action", "medicineModal");
        await Page.Locator("#medicineName").FillAsync("Vitamin D");
        await Page.Locator("#medicineTime").FillAsync("08:15");
        await Page.Locator("#medicineNote").FillAsync("Morning dose");
        await Page.Locator("#medicineModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("medicineModal", open: false);

        var createdMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        var createdMedicine = createdMedicines.Single(x => x.Name == "Vitamin D");
        Assert.That(createdMedicine.Taken, Is.False);

        var medicineRow = Page.Locator(".med-item", new() { HasTextString = "Vitamin D" });
        await medicineRow.Locator(".med-check").ClickAsync();
        await Expect(medicineRow.Locator(".med-check")).ToHaveClassAsync(new Regex("taken"));

        var toggledMedicines = await GetApiAsync<List<MedicineItemDto>>("/api/medicines") ?? [];
        Assert.That(toggledMedicines.Single(x => x.Id == createdMedicine.Id).Taken, Is.True);

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
}
