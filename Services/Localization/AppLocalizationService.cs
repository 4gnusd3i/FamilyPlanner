using System.Globalization;
using System.Text;
using System.Text.Json;
using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;

namespace FamilyPlanner.Services.Localization;

public sealed class AppLocalizationService
{
    private static readonly JsonSerializerOptions SerializerOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    private readonly IReadOnlyDictionary<string, AppLanguagePack> _packs;
    private readonly IConfiguration _configuration;

    public AppLocalizationService(IConfiguration configuration, IWebHostEnvironment environment)
    {
        _configuration = configuration;
        _packs = LoadPacks(Path.Combine(environment.ContentRootPath, "Localization", "LanguagePacks"));

        if (!_packs.ContainsKey("no-NB") || !_packs.ContainsKey("en-US"))
        {
            throw new InvalidOperationException("Language packs for no-NB and en-US are required.");
        }
    }

    public IReadOnlyCollection<string> SupportedLanguageIds => _packs.Keys.ToArray();

    public AppLanguagePack ResolveLanguage(HttpContext? context = null)
    {
        var overrideLanguage = NormalizeLanguageId(_configuration["App:Language"]);
        if (overrideLanguage is not null)
        {
            return _packs[overrideLanguage];
        }

        var systemLanguage = NormalizeLanguageId(CultureInfo.CurrentUICulture.Name);
        if (systemLanguage is not null)
        {
            return _packs[systemLanguage];
        }

        var requestLanguage = ResolveAcceptLanguage(context?.Request.Headers.AcceptLanguage.ToString());
        return requestLanguage is not null ? _packs[requestLanguage] : _packs["no-NB"];
    }

    public AppLanguagePack GetLanguage(string languageId)
    {
        var normalized = NormalizeLanguageId(languageId)
            ?? throw new InvalidOperationException($"Unsupported language id '{languageId}'.");
        return _packs[normalized];
    }

    public string GetString(HttpContext? context, string key) =>
        GetString(ResolveLanguage(context), key);

    public string GetString(AppLanguagePack pack, string key)
    {
        if (pack.Strings.TryGetValue(key, out var value))
        {
            return value;
        }

        if (_packs["no-NB"].Strings.TryGetValue(key, out var fallback))
        {
            return fallback;
        }

        return key;
    }

    public string Format(HttpContext? context, string key, IReadOnlyDictionary<string, string?>? placeholders = null) =>
        Format(ResolveLanguage(context), key, placeholders);

    public string Format(AppLanguagePack pack, string key, IReadOnlyDictionary<string, string?>? placeholders = null)
    {
        var value = GetString(pack, key);
        if (placeholders is null)
        {
            return value;
        }

        foreach (var (placeholder, replacement) in placeholders)
        {
            value = value.Replace($"{{{placeholder}}}", replacement ?? string.Empty, StringComparison.Ordinal);
        }

        return value;
    }

    public PlannerEvent LocalizeGeneratedEvent(AppLanguagePack pack, PlannerEvent plannerEvent, PlannerStore store)
    {
        if (!string.Equals(plannerEvent.SourceType, "birthday", StringComparison.Ordinal) ||
            plannerEvent.SourceMemberId is not > 0)
        {
            return plannerEvent;
        }

        var member = store.GetFamilyMemberById(plannerEvent.SourceMemberId.Value);
        if (member is null)
        {
            return plannerEvent;
        }

        plannerEvent.Title = Format(
            pack,
            "events.birthday_title",
            new Dictionary<string, string?> { ["name"] = member.Name });
        plannerEvent.Note = GetString(pack, "events.birthday_note");
        return plannerEvent;
    }

    private static IReadOnlyDictionary<string, AppLanguagePack> LoadPacks(string directoryPath)
    {
        var packs = new Dictionary<string, AppLanguagePack>(StringComparer.OrdinalIgnoreCase);
        foreach (var filePath in Directory.EnumerateFiles(directoryPath, "*.json", SearchOption.TopDirectoryOnly))
        {
            var json = File.ReadAllText(filePath, Encoding.UTF8);
            var pack = JsonSerializer.Deserialize<AppLanguagePack>(json, SerializerOptions)
                ?? throw new InvalidOperationException($"Failed to deserialize language pack '{filePath}'.");

            ValidatePack(pack, filePath);
            packs[pack.Id] = pack;
        }

        return packs;
    }

    private static void ValidatePack(AppLanguagePack pack, string filePath)
    {
        if (pack.WeekdayNames.Length != 7 || pack.WeekdayShort.Length != 7)
        {
            throw new InvalidOperationException($"Language pack '{filePath}' must define 7 weekday names and 7 weekday short labels.");
        }

        if (pack.MealTypes.Length != 3)
        {
            throw new InvalidOperationException($"Language pack '{filePath}' must define exactly 3 meal types.");
        }
    }

    private static string? ResolveAcceptLanguage(string? header)
    {
        if (string.IsNullOrWhiteSpace(header))
        {
            return null;
        }

        foreach (var part in header.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            var candidate = part.Split(';', 2)[0].Trim();
            var normalized = NormalizeLanguageId(candidate);
            if (normalized is not null)
            {
                return normalized;
            }
        }

        return null;
    }

    private static string? NormalizeLanguageId(string? raw)
    {
        if (string.IsNullOrWhiteSpace(raw))
        {
            return null;
        }

        var normalized = raw.Trim().Replace('_', '-').ToLowerInvariant();
        return normalized switch
        {
            "no-nb" or "nb-no" or "nb" or "no" => "no-NB",
            "en-us" or "en" => "en-US",
            _ => null
        };
    }
}
