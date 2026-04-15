using System.Text;

namespace FamilyPlanner.Endpoints;

public static class PageEndpoints
{
    public static void MapPageEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/", () => HtmlPage("index.html"));
        endpoints.MapGet("/setup", () => HtmlPage("setup.html"));
    }

    private static IResult HtmlPage(string fileName)
    {
        var path = ResolvePagePath(fileName);
        var html = File.ReadAllText(path, Encoding.UTF8);
        return Results.Content(html, "text/html; charset=utf-8", Encoding.UTF8);
    }

    private static string ResolvePagePath(string fileName)
    {
        var baseDirectory = AppContext.BaseDirectory;
        var outputPath = Path.Combine(baseDirectory, "AppPages", fileName);
        if (File.Exists(outputPath))
        {
            return outputPath;
        }

        return Path.Combine(Directory.GetCurrentDirectory(), "AppPages", fileName);
    }
}
