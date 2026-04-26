using System.Text.Json;
using System.Text.Json.Serialization;
using System.Diagnostics;
using Microsoft.Extensions.FileProviders;
using FamilyPlanner.Endpoints;
using FamilyPlanner.Services.Localization;
using FamilyPlanner.Services.Storage;

var builder = WebApplication.CreateBuilder(args);

var configuredUrls = builder.Configuration["App:Urls"] ?? "http://localhost:5080";
var appUrl = GetPrimaryAppUrl(configuredUrls);
var noBrowserRequested = IsNoBrowserRequested(builder.Configuration, Environment.GetCommandLineArgs());

builder.WebHost.UseUrls(configuredUrls);
builder.Logging.ClearProviders();
builder.Logging.AddConsole();
builder.Logging.AddDebug();

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower;
    options.SerializerOptions.DictionaryKeyPolicy = JsonNamingPolicy.SnakeCaseLower;
    options.SerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
});
builder.Services.AddSingleton<AppLocalizationService>();
builder.Services.AddSingleton<LocalizedPageRenderer>();
builder.Services.AddSingleton<StoragePaths>();
builder.Services.AddSingleton<AvatarStorageService>();
builder.Services.AddSingleton<PlannerStore>();

if (!builder.Environment.IsDevelopment() && !noBrowserRequested && IsAppAlreadyRunning(appUrl))
{
    OpenBrowser(appUrl);
    return;
}

var app = builder.Build();

var storagePaths = app.Services.GetRequiredService<StoragePaths>();
Directory.CreateDirectory(storagePaths.RootPath);
Directory.CreateDirectory(storagePaths.UploadsPath);
Directory.CreateDirectory(storagePaths.AvatarsPath);
app.Services.GetRequiredService<PlannerStore>().RunMaintenance();

app.UseStaticFiles();
app.UseStaticFiles(new StaticFileOptions
{
    FileProvider = new PhysicalFileProvider(storagePaths.UploadsPath),
    RequestPath = "/uploads"
});

app.Use(async (context, next) =>
{
    var path = context.Request.Path;
    var store = context.RequestServices.GetRequiredService<PlannerStore>();
    var isConfigured = store.HasHouseholdProfile();
    var isStatic = path.StartsWithSegments("/assets")
                   || path.StartsWithSegments("/pwa")
                   || path.StartsWithSegments("/uploads");

    if (!isConfigured)
    {
        if (path.StartsWithSegments("/api"))
        {
            var localization = context.RequestServices.GetRequiredService<AppLocalizationService>();
            context.Response.StatusCode = StatusCodes.Status409Conflict;
            await context.Response.WriteAsJsonAsync(new { error = localization.GetString(context, "errors.setup_required") });
            return;
        }

        if (!path.StartsWithSegments("/setup") && !isStatic && !path.StartsWithSegments("/health"))
        {
            context.Response.Redirect("/setup");
            return;
        }

        await next();
        return;
    }

    if (path.StartsWithSegments("/setup"))
    {
        context.Response.Redirect("/");
        return;
    }

    await next();
});

app.MapPageEndpoints();
app.MapSetupEndpoints();
app.MapPlannerApiEndpoints();

app.MapGet("/health", () => Results.Ok(new { status = "ok" }));
app.MapFallback(() => Results.Redirect("/"));

OpenBrowserWhenReady(app, appUrl, noBrowserRequested);

app.Run();

static string GetPrimaryAppUrl(string configuredUrls) =>
    configuredUrls
        .Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
        .FirstOrDefault() ?? "http://localhost:5080";

static bool IsNoBrowserRequested(IConfiguration configuration, string[] args) =>
    args.Any(arg => string.Equals(arg, "--no-browser", StringComparison.OrdinalIgnoreCase)) ||
    configuration.GetValue<bool>("App:NoBrowser");

static bool IsAppAlreadyRunning(string appUrl)
{
    try
    {
        using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(2) };
        using var response = client.GetAsync(new Uri(new Uri(appUrl), "health")).GetAwaiter().GetResult();
        return response.IsSuccessStatusCode;
    }
    catch
    {
        return false;
    }
}

static void OpenBrowserWhenReady(WebApplication app, string appUrl, bool noBrowserRequested)
{
    if (app.Environment.IsDevelopment() || noBrowserRequested)
    {
        return;
    }

    app.Lifetime.ApplicationStarted.Register(() =>
    {
        try
        {
            OpenBrowser(appUrl);
        }
        catch (Exception ex)
        {
            app.Logger.LogWarning(ex, "Could not open browser for {AppUrl}", appUrl);
        }
    });
}

static void OpenBrowser(string appUrl)
{
    Process.Start(new ProcessStartInfo
    {
        FileName = appUrl,
        UseShellExecute = true
    });
}
