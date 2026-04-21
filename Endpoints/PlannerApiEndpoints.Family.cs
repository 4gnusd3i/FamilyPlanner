using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static IResult GetFamily(PlannerStore store)
    {
        return Results.Ok(store.GetFamilyMembers());
    }

    private static async Task<IResult> PostFamilyAsync(HttpRequest request, PlannerStore store, AvatarStorageService avatarStorage)
    {
        var form = await request.ReadFormAsync();
        if (form.TryGetValue("delete", out var deleteValue) && deleteValue == "1")
        {
            var id = ParseNullableInt(form["id"]);
            if (id is > 0)
            {
                var existing = store.GetFamilyMemberById(id.Value);
                store.DeleteFamilyMember(id.Value);
                avatarStorage.DeleteIfLocal(existing?.AvatarUrl);
            }

            return Results.Ok(new { ok = true });
        }

        var name = Required(form["name"], "Navn mangler.");
        var memberId = ParseNullableInt(form["id"]);
        var existingMember = memberId is > 0 ? store.GetFamilyMemberById(memberId.Value) : null;
        var avatarUrl = existingMember?.AvatarUrl;

        var uploadedAvatar = form.Files["avatar"];
        if (uploadedAvatar is not null && uploadedAvatar.Length > 0)
        {
            avatarUrl = await avatarStorage.SaveUploadedAsync(uploadedAvatar, existingMember?.AvatarUrl, request.HttpContext.RequestAborted);
        }

        store.UpsertFamilyMember(
            memberId,
            name,
            form["birthday"],
            form["bio"],
            form["color"],
            avatarUrl);

        return Results.Ok(new { ok = true });
    }

}
