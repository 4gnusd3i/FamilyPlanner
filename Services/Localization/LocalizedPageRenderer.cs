using System.Net;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;

namespace FamilyPlanner.Services.Localization;

public sealed partial class LocalizedPageRenderer
{
    private readonly IWebHostEnvironment _environment;
    private readonly AppLocalizationService _localization;

    public LocalizedPageRenderer(IWebHostEnvironment environment, AppLocalizationService localization)
    {
        _environment = environment;
        _localization = localization;
    }

    public async Task WritePageAsync(HttpContext context, string fileName)
    {
        var pack = _localization.ResolveLanguage(context);
        var html = RenderPage(fileName, pack);

        ApplyNoCacheHeaders(context.Response);
        context.Response.Headers["Content-Language"] = pack.Id;
        context.Response.ContentType = "text/html; charset=utf-8";
        await context.Response.WriteAsync(html, Encoding.UTF8);
    }

    public async Task WriteManifestAsync(HttpContext context)
    {
        var pack = _localization.ResolveLanguage(context);
        var manifestJson = RenderManifest(pack);

        ApplyNoCacheHeaders(context.Response);
        context.Response.Headers["Content-Language"] = pack.Id;
        context.Response.ContentType = "application/manifest+json; charset=utf-8";
        await context.Response.WriteAsync(manifestJson, Encoding.UTF8);
    }

    public string RenderPage(string fileName, AppLanguagePack pack)
    {
        var templatePath = Path.Combine(_environment.ContentRootPath, "AppPages", fileName);
        var html = File.ReadAllText(templatePath, Encoding.UTF8);
        html = ApplyTokens(html, pack);
        html = InjectAssetVersions(html);
        return InjectLanguageBootstrap(html, pack);
    }

    public string RenderManifest(AppLanguagePack pack)
    {
        var manifest = new Dictionary<string, object?>
        {
            ["name"] = pack.Manifest.Name,
            ["short_name"] = pack.Manifest.ShortName,
            ["description"] = pack.Manifest.Description,
            ["start_url"] = "/",
            ["display"] = "standalone",
            ["background_color"] = "#f8fafc",
            ["theme_color"] = "#6366f1",
            ["orientation"] = "portrait-primary",
            ["lang"] = pack.HtmlLang,
            ["dir"] = "ltr",
            ["categories"] = new[] { "productivity", "utilities" },
            ["icons"] = new object[]
            {
                new Dictionary<string, string> { ["src"] = "/pwa/icon-72.png", ["sizes"] = "72x72", ["type"] = "image/png" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-96.png", ["sizes"] = "96x96", ["type"] = "image/png" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-128.png", ["sizes"] = "128x128", ["type"] = "image/png" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-144.png", ["sizes"] = "144x144", ["type"] = "image/png" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-152.png", ["sizes"] = "152x152", ["type"] = "image/png" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-192.png", ["sizes"] = "192x192", ["type"] = "image/png", ["purpose"] = "any maskable" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-384.png", ["sizes"] = "384x384", ["type"] = "image/png" },
                new Dictionary<string, string> { ["src"] = "/pwa/icon-512.png", ["sizes"] = "512x512", ["type"] = "image/png" }
            }
        };

        return JsonSerializer.Serialize(manifest);
    }

    private string ApplyTokens(string html, AppLanguagePack pack) =>
        TokenRegex().Replace(
            html,
            match => WebUtility.HtmlEncode(_localization.GetString(pack, match.Groups["key"].Value)));

    private string InjectLanguageBootstrap(string html, AppLanguagePack pack)
    {
        var packJson = JsonSerializer.Serialize(pack);
        var bootstrap = $"""
<script>
window.appLanguagePack = {packJson};
</script>
""";

        return html.Replace("<!--APP_LANGUAGE_BOOTSTRAP-->", bootstrap, StringComparison.Ordinal);
    }

    private string InjectAssetVersions(string html) =>
        AssetRegex().Replace(
            html,
            match =>
            {
                var assetPath = match.Groups["path"].Value;
                var version = GetAssetVersion(assetPath);
                return $"{match.Groups["attr"].Value}=\"{assetPath}?v={version}\"";
            });

    private string GetAssetVersion(string requestPath)
    {
        var relativePath = requestPath.TrimStart('/').Replace('/', Path.DirectorySeparatorChar);
        var webRoot = string.IsNullOrWhiteSpace(_environment.WebRootPath)
            ? Path.Combine(_environment.ContentRootPath, "wwwroot")
            : _environment.WebRootPath;
        var assetPath = Path.Combine(webRoot, relativePath);
        return File.Exists(assetPath)
            ? File.GetLastWriteTimeUtc(assetPath).Ticks.ToString("x")
            : "missing";
    }

    private static void ApplyNoCacheHeaders(HttpResponse response)
    {
        response.Headers.CacheControl = "no-store, no-cache, must-revalidate";
        response.Headers.Pragma = "no-cache";
        response.Headers.Expires = "0";
    }

    [GeneratedRegex("{{(?<key>[A-Za-z0-9_.-]+)}}", RegexOptions.CultureInvariant)]
    private static partial Regex TokenRegex();

    [GeneratedRegex("(?<attr>href|src)=\"(?<path>/(?:assets|pwa)/[^\"?#]+)(?:\\?[^\"#]*)?\"", RegexOptions.CultureInvariant)]
    private static partial Regex AssetRegex();
}
