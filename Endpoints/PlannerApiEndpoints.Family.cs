using System.Text.Json;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.Options;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Endpoints;

public static partial class PlannerApiEndpoints
{
    private static IResult GetFamily(HttpRequest request, PlannerStore store)
    {
        if (request.Query.TryGetValue("assignments", out var assignmentValue) && assignmentValue == "1")
        {
            return Results.Ok(new AssignmentEnvelope { Assignments = store.GetAssignments() });
        }

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

    private static async Task<IResult> PostAssignmentsAsync(HttpRequest request, PlannerStore store)
    {
        var form = await request.ReadFormAsync();
        if (form.TryGetValue("remove_assignment", out var removeValue) && removeValue == "1")
        {
            if (int.TryParse(form["day_of_week"], out var removeDay) && int.TryParse(form["member_id"], out var removeMemberId))
            {
                store.RemoveAssignment(removeDay, removeMemberId);
            }

            return Results.Ok(new { ok = true });
        }

        if (!int.TryParse(form["day_of_week"], out var dayOfWeek) ||
            !int.TryParse(form["member_id"], out var memberId))
        {
            return Results.BadRequest(new { error = "Ugyldig oppgave." });
        }

        var activityType = string.IsNullOrWhiteSpace(form["activity_type"]) ? "medicine" : form["activity_type"].ToString();
        store.UpsertAssignment(dayOfWeek, memberId, activityType, form["note"]);
        return Results.Ok(new { ok = true });
    }

}
