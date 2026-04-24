using FamilyPlanner.Services.Localization;

namespace FamilyPlanner.Endpoints;

public static class PageEndpoints
{
    public static void MapPageEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/", (HttpContext context, LocalizedPageRenderer renderer) => renderer.WritePageAsync(context, "index.html"));
        endpoints.MapGet("/setup", (HttpContext context, LocalizedPageRenderer renderer) => renderer.WritePageAsync(context, "setup.html"));
        endpoints.MapGet("/manifest.webmanifest", (HttpContext context, LocalizedPageRenderer renderer) => renderer.WriteManifestAsync(context));
    }
}
