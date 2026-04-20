using System.Text;
using System.Text.RegularExpressions;

namespace FamilyPlanner.Endpoints;

public static class PageEndpoints
{
    public static void MapPageEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/", (HttpContext context) => WriteHtmlPageAsync(context, "index.html"));
        endpoints.MapGet("/setup", (HttpContext context) => WriteHtmlPageAsync(context, "setup.html"));
    }

    private static async Task WriteHtmlPageAsync(HttpContext context, string fileName)
    {
        var path = ResolvePagePath(fileName);
        var html = InjectAssetVersions(File.ReadAllText(path, Encoding.UTF8));

        context.Response.Headers.CacheControl = "no-store, no-cache, must-revalidate";
        context.Response.Headers.Pragma = "no-cache";
        context.Response.Headers.Expires = "0";
        context.Response.ContentType = "text/html; charset=utf-8";
        await context.Response.WriteAsync(html, Encoding.UTF8);
    }

    private static string ResolvePagePath(string fileName)
    {
        return ResolveContentPath("AppPages", fileName)
            ?? throw new FileNotFoundException($"Could not locate AppPages/{fileName}.");
    }

    private static string InjectAssetVersions(string html) =>
        Regex.Replace(
            html,
            "(?<attr>href|src)=\"(?<path>/(?:assets|pwa)/[^\"?#]+)(?:\\?[^\"#]*)?\"",
            match =>
            {
                var assetPath = match.Groups["path"].Value;
                var version = GetAssetVersion(assetPath);
                return $"{match.Groups["attr"].Value}=\"{assetPath}?v={version}\"";
            },
            RegexOptions.CultureInvariant);

    private static string GetAssetVersion(string requestPath)
    {
        var relativePath = requestPath.TrimStart('/').Replace('/', Path.DirectorySeparatorChar);
        var assetPath = ResolveContentPath("wwwroot", relativePath, throwIfMissing: false);
        if (assetPath is null)
        {
            return "missing";
        }

        return File.GetLastWriteTimeUtc(assetPath).Ticks.ToString("x");
    }

    private static string? ResolveContentPath(string rootFolder, string relativePath, bool throwIfMissing = true)
    {
        foreach (var contentRoot in EnumerateCandidateRoots())
        {
            var candidate = Path.Combine(contentRoot, rootFolder, relativePath);
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        if (!throwIfMissing)
        {
            return null;
        }

        throw new FileNotFoundException($"Could not locate {rootFolder}/{relativePath}.");
    }

    private static IEnumerable<string> EnumerateCandidateRoots()
    {
        var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var roots = new[]
        {
            AppContext.BaseDirectory,
            Directory.GetCurrentDirectory(),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..")),
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..")),
            Path.GetFullPath(Path.Combine(Directory.GetCurrentDirectory(), "..", "..", "..")),
            Path.GetFullPath(Path.Combine(Directory.GetCurrentDirectory(), "..", "..", "..", ".."))
        };

        foreach (var root in roots)
        {
            if (seen.Add(root))
            {
                yield return root;
            }
        }
    }
}
