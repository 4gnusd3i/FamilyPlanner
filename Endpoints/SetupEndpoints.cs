using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static class SetupEndpoints
{
    public static void MapSetupEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapPost("/setup", SetupAsync);
    }

    private static async Task<IResult> SetupAsync(
        HttpContext context,
        PlannerStore store,
        AvatarStorageService avatarStorage)
    {
        if (store.HasHouseholdProfile())
        {
            return Results.Redirect("/");
        }

        var form = await context.Request.ReadFormAsync();
        var familyName = form["family_name"].ToString().Trim();
        var firstMemberName = form["first_member_name"].ToString().Trim();
        var birthday = form["first_member_birthday"].ToString();
        var bio = form["first_member_bio"].ToString();
        var color = form["first_member_color"].ToString();

        if (string.IsNullOrWhiteSpace(familyName) || string.IsNullOrWhiteSpace(firstMemberName))
        {
            return Results.Redirect("/setup?error=missing");
        }

        string? avatarUrl = null;
        var uploadedAvatar = form.Files["first_member_avatar"];
        if (uploadedAvatar is not null && uploadedAvatar.Length > 0)
        {
            avatarUrl = await avatarStorage.SaveUploadedAsync(uploadedAvatar, currentAvatarUrl: null, context.RequestAborted);
        }

        // Ensure first-time setup starts from a clean local household state.
        store.ResetForFreshSetup();
        store.InitializeHousehold(familyName);
        store.UpsertFamilyMember(
            id: null,
            name: firstMemberName,
            birthday,
            bio,
            color,
            avatarUrl);

        return Results.Redirect("/");
    }
}
