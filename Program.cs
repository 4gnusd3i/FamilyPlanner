using System.Text.Json;
using System.Text.Json.Serialization;
using System.Diagnostics;
using Microsoft.Extensions.FileProviders;
using FamilyPlanner.Endpoints;
using FamilyPlanner.Services.Localization;
using FamilyPlanner.Services.Storage;

var builder = WebApplication.CreateBuilder(args);

builder.WebHost.UseUrls(builder.Configuration["App:Urls"] ?? "http://localhost:5080");
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

var app = builder.Build();
var appUrl = builder.Configuration["App:Urls"] ?? "http://localhost:5080";

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

OpenBrowserWhenReady(app, appUrl);

app.Run();

static void OpenBrowserWhenReady(WebApplication app, string appUrl)
{
    var configuration = app.Configuration;
    if (app.Environment.IsDevelopment() ||
        argsContainNoBrowser(Environment.GetCommandLineArgs()) ||
        configuration.GetValue<bool>("App:NoBrowser")) {
        return;
    }

    app.Lifetime.ApplicationStarted.Register(() =>
    {
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = appUrl,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            app.Logger.LogWarning(ex, "Could not open browser for {AppUrl}", appUrl);
        }
    });

    static bool argsContainNoBrowser(string[] args) =>
        args.Any(arg => string.Equals(arg, "--no-browser", StringComparison.OrdinalIgnoreCase));
}
