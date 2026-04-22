using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class NoteWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task NoteCrud_ViewAndEdit_PersistCorrectly()
    {
        await OpenModalBySelectorAsync(".quick-action:has-text('Notat')", "noteModal");
        await Page.Locator("#noteTitle").FillAsync("Packing list");
        await Page.Locator("#noteContent").FillAsync("Shoes and water bottle");
        await Page.Locator("#noteModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("noteModal", open: false);

        var createdNotes = await GetApiAsync<List<NoteItemDto>>("/api/notes") ?? [];
        var createdNote = createdNotes.Single(x => x.Title == "Packing list");

        await Page.Locator(".note-item", new() { HasTextString = "Packing list" }).ClickAsync();
        await WaitForModalStateAsync("entryViewModal", open: true);
        await Expect(Page.Locator("#entryViewContent")).ToContainTextAsync("Shoes and water bottle");
        await Page.Locator("#entryViewModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("noteModal", open: true);
        await Page.Locator("#noteContent").FillAsync("Shoes, water bottle and jacket");
        await Page.Locator("#noteModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("noteModal", open: false);

        var updatedNotes = await GetApiAsync<List<NoteItemDto>>("/api/notes") ?? [];
        Assert.That(updatedNotes.Single(x => x.Id == createdNote.Id).Content, Is.EqualTo("Shoes, water bottle and jacket"));

        await Page.Locator(".note-item", new() { HasTextString = "Packing list" }).ClickAsync();
        await WaitForModalStateAsync("entryViewModal", open: true);
        await Page.Locator("#entryViewModal .btn-danger").ClickAsync();
        await WaitForModalStateAsync("entryViewModal", open: false);
        await Expect(Page.Locator(".note-item", new() { HasTextString = "Packing list" })).ToHaveCountAsync(0);

        var finalNotes = await GetApiAsync<List<NoteItemDto>>("/api/notes") ?? [];
        Assert.That(finalNotes.Any(x => x.Id == createdNote.Id), Is.False);
    }
}
