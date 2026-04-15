using NUnit.Framework;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class FamilyWorkflowTests : DesktopPlannerUiTestBase
{
    [Test]
    public async Task FamilyCrud_ProfileAndAssignments_PersistCorrectly()
    {
        var avatarPath = Path.Combine(UiTestHost.RepositoryRoot, "wwwroot", "pwa", "icon-72.png");

        await Page.Locator(".add-family-btn").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: true);
        await Page.Locator("#memberAvatar").SetInputFilesAsync(avatarPath);
        await Page.Locator("#memberName").FillAsync("Emma");
        await Page.Locator("#memberBirthday").FillAsync("2018-06-14");
        await Page.Locator("#memberBio").FillAsync("Loves dance.");
        await Page.Locator("#memberModal .color-opt").Nth(2).ClickAsync();
        await Page.Locator("#memberModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: false);

        await Expect(Page.Locator("#familyCountLabel")).ToHaveTextAsync("3");

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

        await DragFamilyMemberToDayAsync("Emma", 2);
        await WaitForModalStateAsync("assignModal", open: true);
        await Page.Locator("#assignModal .act-btn[data-type='doctor']").ClickAsync();
        await Page.Locator("#assignNote").FillAsync("Dental check");
        await Page.Locator("#assignModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("assignModal", open: false);

        var assignmentEnvelope = await GetApiAsync<AssignmentEnvelopeDto>("/api/family/assignments")
            ?? throw new AssertionException("Assignment response was null.");
        Assert.That(
            assignmentEnvelope.Assignments.Any(x =>
                x.DayOfWeek == 2 &&
                x.FamilyMemberId == emma.Id &&
                x.ActivityType == "doctor" &&
                x.Note == "Dental check"),
            Is.True);

        await Page.Locator("#tasks-2 .task-chip").Last.ClickAsync();
        await WaitForModalStateAsync("assignModal", open: true);
        await Page.Locator("#removeAssignBtn").ClickAsync();
        await WaitForModalStateAsync("assignModal", open: false);

        var afterRemoval = await GetApiAsync<AssignmentEnvelopeDto>("/api/family/assignments")
            ?? throw new AssertionException("Assignment response after removal was null.");
        Assert.That(afterRemoval.Assignments.Any(x => x.FamilyMemberId == emma.Id && x.DayOfWeek == 2), Is.False);

        await Page.Locator(".family-avatar", new() { HasTextString = "Emma" }).ClickAsync();
        await WaitForModalStateAsync("profileModal", open: true);
        await Page.Locator("#profileModal .btn-primary").ClickAsync();
        await WaitForModalStateAsync("memberModal", open: true);
        await AcceptDialogAsync(
            () => Page.Locator("#deleteMemberBtn").ClickAsync(),
            "Slette");

        await Expect(Page.Locator("#familyCountLabel")).ToHaveTextAsync("2");
        var finalFamily = await GetApiAsync<List<FamilyMemberDto>>("/api/family") ?? [];
        Assert.That(finalFamily.Any(x => x.Id == emma.Id), Is.False);
    }
}
