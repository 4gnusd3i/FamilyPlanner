using NUnit.Framework;

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
        await WaitForModalStateAsync("noteViewModal", open: true);
        await Page.Locator("#noteViewModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("noteModal", open: true);
        await Page.Locator("#noteContent").FillAsync("Shoes, water bottle and jacket");
        await Page.Locator("#noteModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("noteModal", open: false);

        var updatedNotes = await GetApiAsync<List<NoteItemDto>>("/api/notes") ?? [];
        Assert.That(updatedNotes.Single(x => x.Id == createdNote.Id).Content, Is.EqualTo("Shoes, water bottle and jacket"));

        await Page.Locator(".note-item", new() { HasTextString = "Packing list" }).ClickAsync();
        await WaitForModalStateAsync("noteViewModal", open: true);
        await AcceptDialogAsync(
            () => Page.Locator("#noteViewModal .btn-danger").ClickAsync(),
            "Slette");
        await WaitForModalStateAsync("noteViewModal", open: false);
        await Expect(Page.Locator(".note-item", new() { HasTextString = "Packing list" })).ToHaveCountAsync(0);

        var finalNotes = await GetApiAsync<List<NoteItemDto>>("/api/notes") ?? [];
        Assert.That(finalNotes.Any(x => x.Id == createdNote.Id), Is.False);
    }
}
