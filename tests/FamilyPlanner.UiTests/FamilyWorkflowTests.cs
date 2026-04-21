using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class FamilyWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task FamilyCrud_ProfileAndDragToEvent_PersistCorrectly()
    {
        var avatarPath = Path.Combine(UiTestHost.RepositoryRoot, "wwwroot", "pwa", "icon-72.png");

        await Page.Locator(".add-family-btn").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: true);
        await Page.Locator("#memberAvatar").SetInputFilesAsync(avatarPath);
        await Page.Locator("#memberName").FillAsync("Emma");
        await Page.Locator("#memberBirthday").FillAsync("2018-06-14");
        await Page.Locator("#memberBio").FillAsync("Loves dance.");
        await Expect(Page.Locator("#memberModal .color-opt")).ToHaveCountAsync(20);
        await Page.Locator("#memberModal .color-opt").Nth(2).ClickAsync();
        await Page.Locator("#memberModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: false);

        await Expect(Page.Locator(".family-avatar", new() { HasTextString = "Emma" })).ToBeVisibleAsync();

        var createdFamily = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        var emma = createdFamily.Single(x => x.Name == "Emma");
        Assert.That(emma.AvatarUrl, Is.Not.Null.And.Not.Empty);

        var emmaAvatar = Page.Locator(".family-avatar", new() { HasTextString = "Emma" });
        await emmaAvatar.ClickAsync();
        await WaitForModalStateAsync("profileModal", open: true);
        await Expect(Page.Locator("#profileBio")).ToContainTextAsync("Loves dance.");
        await Page.Locator("#profileModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: true);
        await Page.Locator("#memberBio").FillAsync("Loves dance and books.");
        await Page.Locator("#memberModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: false);

        var updatedFamily = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        emma = updatedFamily.Single(x => x.Id == emma.Id);
        Assert.That(emma.Bio, Is.EqualTo("Loves dance and books."));

        var targetDate = await Page.Locator(".day-box[data-day='2']").GetAttributeAsync("data-date")
            ?? throw new AssertionException("Target day date was missing.");
        await DragFamilyMemberToDayAsync("Emma", 2);
        await WaitForModalStateAsync("eventModal", open: true);
        await Expect(Page.Locator("#eventDate")).ToHaveValueAsync(targetDate);
        await Expect(Page.Locator("#eventOwner")).ToHaveValueAsync(emma.Id.ToString());
        await Page.Locator("#eventTitle").FillAsync("Dental check");
        await Page.Locator("#eventNote").FillAsync("Remember insurance card.");
        await Page.Locator("#eventModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("eventModal", open: false);

        var draggedEvents = await GetApiAsync<List<PlannerEventDto>>($"/api/events?start={targetDate}&end={targetDate}") ?? [];
        var draggedEvent = draggedEvents.Single(x => x.Title == "Dental check");
        Assert.Multiple(() =>
        {
            Assert.That(draggedEvent.OwnerId, Is.EqualTo(emma.Id));
            Assert.That(draggedEvent.Color, Is.EqualTo(emma.Color));
            Assert.That(draggedEvent.Note, Is.EqualTo("Remember insurance card."));
        });

        await Page.Locator(".family-avatar", new() { HasTextString = "Emma" }).ClickAsync();
        await WaitForModalStateAsync("profileModal", open: true);
        await Page.Locator("#profileModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: true);
        await AcceptDialogAsync(
            () => Page.Locator("#deleteMemberBtn").ClickAsync(),
            "Slette");

        await Expect(Page.Locator(".family-avatar", new() { HasTextString = "Emma" })).ToHaveCountAsync(0);
        var finalFamily = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        Assert.That(finalFamily.Any(x => x.Id == emma.Id), Is.False);
    }
}
