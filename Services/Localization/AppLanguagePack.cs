using System.Text.Json.Serialization;

namespace FamilyPlanner.Services.Localization;

public sealed class AppLanguagePack
{
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    [JsonPropertyName("locale")]
    public required string Locale { get; init; }

    [JsonPropertyName("html_lang")]
    public required string HtmlLang { get; init; }

    [JsonPropertyName("manifest")]
    public required AppManifestPack Manifest { get; init; }

    [JsonPropertyName("weekday_names")]
    public required string[] WeekdayNames { get; init; }

    [JsonPropertyName("weekday_short")]
    public required string[] WeekdayShort { get; init; }

    [JsonPropertyName("meal_types")]
    public required AppMealTypePack[] MealTypes { get; init; }

    [JsonPropertyName("strings")]
    public required Dictionary<string, string> Strings { get; init; }
}

public sealed class AppManifestPack
{
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    [JsonPropertyName("short_name")]
    public required string ShortName { get; init; }

    [JsonPropertyName("description")]
    public required string Description { get; init; }
}

public sealed class AppMealTypePack
{
    [JsonPropertyName("key")]
    public required string Key { get; init; }

    [JsonPropertyName("label")]
    public required string Label { get; init; }

    [JsonPropertyName("name")]
    public required string Name { get; init; }

    [JsonPropertyName("option_label")]
    public required string OptionLabel { get; init; }
}
