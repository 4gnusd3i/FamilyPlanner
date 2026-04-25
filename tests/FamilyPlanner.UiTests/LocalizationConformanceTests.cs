using System.Globalization;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Localization;
using FamilyPlanner.Services.Storage;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.FileProviders;
using NUnit.Framework;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class LocalizationConformanceTests
{
    [Test]
    public void LanguageResolution_PrefersLaunchOverrideOverBrowserLanguage()
    {
        var service = CreateLocalizationService("en-US");
        var context = new DefaultHttpContext();
        context.Request.Headers.AcceptLanguage = "nb-NO,nb;q=0.9";

        var language = service.ResolveLanguage(context);

        Assert.That(language.Id, Is.EqualTo("en-US"));
    }

    [Test]
    [NonParallelizable]
    public void LanguageResolution_UsesBrowserLanguageWhenSystemCultureIsUnsupported()
    {
        var originalCulture = CultureInfo.CurrentCulture;
        var originalUiCulture = CultureInfo.CurrentUICulture;

        try
        {
            var unsupportedCulture = CultureInfo.GetCultureInfo("fr-FR");
            CultureInfo.CurrentCulture = unsupportedCulture;
            CultureInfo.CurrentUICulture = unsupportedCulture;

            var service = CreateLocalizationService();
            var context = new DefaultHttpContext();
            context.Request.Headers.AcceptLanguage = "nb-NO,nb;q=0.9,en-US;q=0.7";

            var language = service.ResolveLanguage(context);

            Assert.That(language.Id, Is.EqualTo("no-NB"));
        }
        finally
        {
            CultureInfo.CurrentCulture = originalCulture;
            CultureInfo.CurrentUICulture = originalUiCulture;
        }
    }

    [Test]
    [NonParallelizable]
    public void LanguageResolution_FallsBackToSystemCultureWhenNoOverrideOrBrowserLanguage()
    {
        var originalCulture = CultureInfo.CurrentCulture;
        var originalUiCulture = CultureInfo.CurrentUICulture;

        try
        {
            var englishCulture = CultureInfo.GetCultureInfo("en-US");
            CultureInfo.CurrentCulture = englishCulture;
            CultureInfo.CurrentUICulture = englishCulture;

            var service = CreateLocalizationService();
            var language = service.ResolveLanguage();

            Assert.That(language.Id, Is.EqualTo("en-US"));
        }
        finally
        {
            CultureInfo.CurrentCulture = originalCulture;
            CultureInfo.CurrentUICulture = originalUiCulture;
        }
    }

    [Test]
    public void PageRenderer_RendersEnglishPageAndManifestWithoutTemplateTokens()
    {
        var service = CreateLocalizationService("en-US");
        var renderer = CreateRenderer(service);
        var pack = service.ResolveLanguage();

        var html = renderer.RenderPage("index.html", pack);
        var manifestJson = renderer.RenderManifest(pack);
        using var manifest = JsonDocument.Parse(manifestJson);

        Assert.Multiple(() =>
        {
            Assert.That(html, Does.Contain("<html lang=\"en\">"));
            Assert.That(html, Does.Contain("+ Meal"));
            Assert.That(html, Does.Contain("Shopping list"));
            Assert.That(html, Does.Contain("window.appLanguagePack"));
            Assert.That(html, Does.Not.Contain("{{"));
            Assert.That(manifest.RootElement.GetProperty("description").GetString(), Is.EqualTo("Simple weekly planning for the family"));
            Assert.That(manifest.RootElement.GetProperty("lang").GetString(), Is.EqualTo("en"));
        });
    }

    [Test]
    public void StaticLegacyManifest_IsRemovedInFavorOfLocalizedDynamicManifest()
    {
        var legacyManifestPath = Path.Combine(UiTestHost.RepositoryRoot, "wwwroot", "pwa", "manifest.json");
        var service = CreateLocalizationService("no-NB");
        var renderer = CreateRenderer(service);
        using var manifest = JsonDocument.Parse(renderer.RenderManifest(service.ResolveLanguage()));

        Assert.Multiple(() =>
        {
            Assert.That(File.Exists(legacyManifestPath), Is.False, "The stale static manifest should not coexist with /manifest.webmanifest.");
            Assert.That(manifest.RootElement.GetProperty("name").GetString(), Is.EqualTo("FamilyPlanner - Ukesplanlegger"));
            Assert.That(manifest.RootElement.GetProperty("lang").GetString(), Is.EqualTo("nb"));
        });
    }

    [Test]
    public void LanguagePacks_ShareTheSameKeys_AndCoverAllTemplateAndClientTranslations()
    {
        var service = CreateLocalizationService();
        var norwegianPack = service.GetLanguage("no-NB");
        var englishPack = service.GetLanguage("en-US");
        var comparer = StringComparer.Ordinal;

        var norwegianKeys = norwegianPack.Strings.Keys.OrderBy(key => key, comparer).ToArray();
        var englishKeys = englishPack.Strings.Keys.OrderBy(key => key, comparer).ToArray();

        Assert.That(englishKeys, Is.EqualTo(norwegianKeys), "Language packs must expose the same translation keys.");

        var referencedKeys = ExtractTemplateKeys()
            .Concat(ExtractClientTranslationKeys())
            .Distinct(comparer)
            .OrderBy(key => key, comparer)
            .ToArray();

        var missingNorwegian = referencedKeys
            .Where(key => !norwegianPack.Strings.ContainsKey(key))
            .ToArray();
        var missingEnglish = referencedKeys
            .Where(key => !englishPack.Strings.ContainsKey(key))
            .ToArray();

        Assert.Multiple(() =>
        {
            Assert.That(missingNorwegian, Is.Empty, $"Missing no-NB translations: {string.Join(", ", missingNorwegian)}");
            Assert.That(missingEnglish, Is.Empty, $"Missing en-US translations: {string.Join(", ", missingEnglish)}");
        });
    }

    [Test]
    public void GeneratedBirthdayEvents_AreLocalizedPerLanguagePack()
    {
        using var tempDirectory = new TemporaryDirectory();
        var config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["App:DataRoot"] = tempDirectory.Path
            })
            .Build();

        var storagePaths = new StoragePaths(config);
        Directory.CreateDirectory(storagePaths.RootPath);
        Directory.CreateDirectory(storagePaths.AvatarsPath);

        using var store = new PlannerStore(storagePaths);
        var member = store.UpsertFamilyMember(null, "Anna", "2015-04-15", null, "#ffafcc", null);
        var service = CreateLocalizationService("en-US");
        var pack = service.ResolveLanguage();
        var plannerEvent = new PlannerEvent
        {
            SourceType = "birthday",
            SourceMemberId = member.Id,
            Title = "Placeholder",
            Note = "Placeholder"
        };

        var localizedEvent = service.LocalizeGeneratedEvent(pack, plannerEvent, store);

        Assert.Multiple(() =>
        {
            Assert.That(localizedEvent.Title, Is.EqualTo("Anna's birthday"));
            Assert.That(localizedEvent.Note, Is.EqualTo("Birthday"));
        });
    }

    [Test]
    public void RepositoryTextFiles_AreUtf8WithoutBom()
    {
        var utf8 = new UTF8Encoding(encoderShouldEmitUTF8Identifier: false, throwOnInvalidBytes: true);

        foreach (var filePath in EnumerateRepositoryTextFiles())
        {
            var bytes = File.ReadAllBytes(filePath);

            Assert.That(
                HasUtf8Bom(bytes),
                Is.False,
                $"{filePath} should be UTF-8 without BOM to avoid inconsistent encoding behavior.");

            Assert.DoesNotThrow(
                () => utf8.GetString(bytes),
                $"{filePath} should decode as valid UTF-8.");
        }
    }

    private static AppLocalizationService CreateLocalizationService(string? overrideLanguage = null)
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["App:Language"] = overrideLanguage
            })
            .Build();

        return new AppLocalizationService(configuration, CreateEnvironment());
    }

    private static LocalizedPageRenderer CreateRenderer(AppLocalizationService service) =>
        new(CreateEnvironment(), service);

    private static TestWebHostEnvironment CreateEnvironment() =>
        new(UiTestHost.RepositoryRoot);

    private static IEnumerable<string> EnumerateRepositoryTextFiles()
    {
        var root = UiTestHost.RepositoryRoot;
        var directories = new[]
        {
            "AppPages",
            "Endpoints",
            "Localization",
            "Models",
            "Services",
            "tests",
            "wwwroot"
        };

        foreach (var directory in directories)
        {
            var fullPath = Path.Combine(root, directory);
            if (!Directory.Exists(fullPath))
            {
                continue;
            }

            foreach (var filePath in Directory.EnumerateFiles(fullPath, "*", SearchOption.AllDirectories))
            {
                if (IsIgnoredPath(filePath))
                {
                    continue;
                }

                if (IsTextFile(filePath))
                {
                    yield return filePath;
                }
            }
        }

        foreach (var fileName in new[]
        {
            ".editorconfig",
            ".gitattributes",
            ".gitignore",
            "Program.cs",
            "FamilyPlanner.csproj",
            "Launch-FamilyPlanner.ps1",
            "Launch-FamilyPlanner.cmd",
            "Run-UiRegression.ps1",
            "Install-UiRegressionBrowser.ps1",
            "AGENTS.md",
            "README.md"
        })
        {
            var path = Path.Combine(root, fileName);
            if (File.Exists(path))
            {
                yield return path;
            }
        }
    }

    private static bool IsIgnoredPath(string filePath) =>
        filePath.Contains($"{Path.DirectorySeparatorChar}bin{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}obj{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}.artifacts{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}.playwright-browsers{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}.dotnet{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}.localdata{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}.smoketestdata{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}.smoketestdata-ui{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase) ||
        filePath.Contains($"{Path.DirectorySeparatorChar}TestResults{Path.DirectorySeparatorChar}", StringComparison.OrdinalIgnoreCase);

    private static bool IsTextFile(string filePath)
    {
        var fileName = Path.GetFileName(filePath);
        var extension = Path.GetExtension(filePath);
        return fileName is ".editorconfig" or ".gitattributes" or ".gitignore" ||
               extension is ".cs" or ".csproj" or ".html" or ".js" or ".css" or ".json" or ".md" or ".ps1" or ".cmd" or ".bat";
    }

    private static bool HasUtf8Bom(byte[] bytes) =>
        bytes.Length >= 3 &&
        bytes[0] == 0xEF &&
        bytes[1] == 0xBB &&
        bytes[2] == 0xBF;

    private static IEnumerable<string> ExtractTemplateKeys()
    {
        var regex = new Regex(@"{{(?<key>[A-Za-z0-9_.-]+)}}", RegexOptions.CultureInvariant);
        var appPagesPath = Path.Combine(UiTestHost.RepositoryRoot, "AppPages");
        return ExtractKeys(appPagesPath, "*.html", regex);
    }

    private static IEnumerable<string> ExtractClientTranslationKeys()
    {
        var regex = new Regex(@"(?<![A-Za-z0-9_$])t\(\s*[""'](?<key>[A-Za-z0-9_.-]+)[""']", RegexOptions.CultureInvariant);
        var scriptsPath = Path.Combine(UiTestHost.RepositoryRoot, "wwwroot", "assets", "js");
        return ExtractKeys(scriptsPath, "*.js", regex);
    }

    private static IEnumerable<string> ExtractKeys(string rootPath, string searchPattern, Regex regex)
    {
        if (!Directory.Exists(rootPath))
        {
            return [];
        }

        return Directory
            .EnumerateFiles(rootPath, searchPattern, SearchOption.AllDirectories)
            .SelectMany(filePath => regex.Matches(File.ReadAllText(filePath, Encoding.UTF8)).Select(match => match.Groups["key"].Value))
            .Where(key => !string.IsNullOrWhiteSpace(key));
    }

    private sealed class TestWebHostEnvironment : IWebHostEnvironment
    {
        public TestWebHostEnvironment(string contentRootPath)
        {
            ContentRootPath = contentRootPath;
            ContentRootFileProvider = new PhysicalFileProvider(contentRootPath);
            WebRootPath = Path.Combine(contentRootPath, "wwwroot");
            WebRootFileProvider = new PhysicalFileProvider(WebRootPath);
        }

        public string ApplicationName { get; set; } = "FamilyPlanner.Tests";
        public IFileProvider WebRootFileProvider { get; set; }
        public string WebRootPath { get; set; }
        public string EnvironmentName { get; set; } = "Development";
        public string ContentRootPath { get; set; }
        public IFileProvider ContentRootFileProvider { get; set; }
    }

    private sealed class TemporaryDirectory : IDisposable
    {
        public TemporaryDirectory()
        {
            Path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), $"familyplanner-localization-{Guid.NewGuid():N}");
            Directory.CreateDirectory(Path);
        }

        public string Path { get; }

        public void Dispose()
        {
            if (Directory.Exists(Path))
            {
                Directory.Delete(Path, recursive: true);
            }
        }
    }
}
