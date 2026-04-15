using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.FileProviders;
using FamilyPlanner.Endpoints;
using FamilyPlanner.Services.Storage;

var builder = WebApplication.CreateBuilder(args);
var dataRoot = ResolveDataRoot(builder.Configuration);

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
builder.Services.AddSingleton<StoragePaths>();
builder.Services.AddSingleton<AvatarStorageService>();
builder.Services.AddSingleton<PlannerStore>();

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
            context.Response.StatusCode = StatusCodes.Status409Conflict;
            await context.Response.WriteAsJsonAsync(new { error = "setup_required" });
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

app.Run();

static string ResolveDataRoot(IConfiguration configuration)
{
    var configuredRoot = configuration["App:DataRoot"];
    var environmentRoot = Environment.GetEnvironmentVariable("FAMILYPLANNER_DATA_ROOT");

    return string.IsNullOrWhiteSpace(environmentRoot)
        ? string.IsNullOrWhiteSpace(configuredRoot)
            ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "FamilyPlanner")
            : configuredRoot
        : environmentRoot;
}
