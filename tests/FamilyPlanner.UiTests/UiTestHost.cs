using System.Diagnostics;
using System.Net;
using System.Net.Http.Json;
using System.Net.Sockets;
using System.Text.Json.Serialization;

namespace FamilyPlanner.UiTests;

internal static class UiTestHost
{
    private static readonly SemaphoreSlim SyncLock = new(1, 1);
    private static Process? _process;
    private static int? _port;
    private static int _instanceSequence;

    public static string RepositoryRoot { get; } = ResolveRepositoryRoot();
    public static string ProjectPath { get; } = Path.Combine(RepositoryRoot, "FamilyPlanner.csproj");
    public static string ArtifactsRoot { get; } = Path.Combine(
        RepositoryRoot,
        "tests",
        "FamilyPlanner.UiTests",
        ".artifacts",
        DateTime.UtcNow.ToString("yyyyMMdd-HHmmss"));

    public static string DataRoot { get; private set; } = string.Empty;
    public static string BrowserPath { get; } = Path.Combine(RepositoryRoot, ".playwright-browsers");
    public static string FamilyName => "Regressionfamilien";
    public static string BaseUrl { get; private set; } = string.Empty;

    public static async Task EnsureStartedAsync()
    {
        await SyncLock.WaitAsync();
        try
        {
            if (_process is { HasExited: false })
            {
                return;
            }

            await StartOrRestartAsync(resetData: true);
        }
        finally
        {
            SyncLock.Release();
        }
    }

    public static async Task ResetStateAsync()
    {
        await SyncLock.WaitAsync();
        try
        {
            await StartOrRestartAsync(resetData: true);
        }
        finally
        {
            SyncLock.Release();
        }
    }

    public static async Task StopAsync()
    {
        await SyncLock.WaitAsync();
        try
        {
            if (_process is null)
            {
                return;
            }

            if (!_process.HasExited)
            {
                _process.Kill(entireProcessTree: true);
                await _process.WaitForExitAsync();
            }

            _process.Dispose();
            _process = null;
        }
        finally
        {
            SyncLock.Release();
        }
    }

    public static Task<HttpClient> CreateClientAsync() => Task.FromResult(CreateClient());

    private static async Task StartOrRestartAsync(bool resetData)
    {
        Directory.CreateDirectory(ArtifactsRoot);
        Directory.CreateDirectory(BrowserPath);

        if (_port is null)
        {
            _port = GetFreeTcpPort();
            BaseUrl = $"http://127.0.0.1:{_port.Value}";
        }

        await StopProcessUnlockedAsync();

        if (resetData || string.IsNullOrWhiteSpace(DataRoot))
        {
            var instanceId = Interlocked.Increment(ref _instanceSequence);
            DataRoot = Path.Combine(ArtifactsRoot, $"appdata-{instanceId:000}");
        }

        Directory.CreateDirectory(DataRoot);

        var startInfo = new ProcessStartInfo
        {
            FileName = "dotnet",
            WorkingDirectory = RepositoryRoot,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
        };

        startInfo.ArgumentList.Add("run");
        startInfo.ArgumentList.Add("--project");
        startInfo.ArgumentList.Add(ProjectPath);
        startInfo.ArgumentList.Add("--no-build");
        startInfo.ArgumentList.Add("--no-launch-profile");

        startInfo.Environment["DOTNET_SKIP_FIRST_TIME_EXPERIENCE"] = "1";
        startInfo.Environment["DOTNET_CLI_TELEMETRY_OPTOUT"] = "1";
        startInfo.Environment["DOTNET_CLI_HOME"] = Path.Combine(RepositoryRoot, ".dotnet");
        startInfo.Environment["FAMILYPLANNER_DATA_ROOT"] = DataRoot;
        startInfo.Environment["PLAYWRIGHT_BROWSERS_PATH"] = BrowserPath;
        startInfo.Environment["App__Urls"] = BaseUrl;

        _process = Process.Start(startInfo) ?? throw new InvalidOperationException("Failed to start local app process.");
        _ = PumpStreamAsync(_process.StandardOutput, Path.Combine(ArtifactsRoot, "app.stdout.log"));
        _ = PumpStreamAsync(_process.StandardError, Path.Combine(ArtifactsRoot, "app.stderr.log"));

        await WaitForHealthyAsync();
        await SeedAsync();
    }

    private static async Task StopProcessUnlockedAsync()
    {
        if (_process is null)
        {
            return;
        }

        if (!_process.HasExited)
        {
            _process.Kill(entireProcessTree: true);
            await _process.WaitForExitAsync();
        }

        _process.Dispose();
        _process = null;
    }

    private static async Task SeedAsync()
    {
        using var client = CreateClient();

        var setupResponse = await client.PostAsync(
            "/setup",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["family_name"] = FamilyName,
                ["first_member_name"] = "Anna",
                ["first_member_birthday"] = DateOnly.FromDateTime(DateTime.Today.AddYears(-12)).ToString("yyyy-MM-dd"),
                ["first_member_bio"] = "Liker handball og faste rutiner.",
                ["first_member_color"] = "#e4f4ef",
            }));

        EnsureSuccessOrRedirect(setupResponse, "setup");

        await PostFormAsync(client, "/api/family", new Dictionary<string, string>
        {
            ["name"] = "Oskar",
            ["birthday"] = DateOnly.FromDateTime(DateTime.Today.AddYears(-9)).ToString("yyyy-MM-dd"),
            ["bio"] = "Skole, trommer og fredagstaco.",
            ["color"] = "#f8ead8",
        });

        var family = await client.GetFromJsonAsync<List<FamilyMemberDto>>("/api/family")
            ?? throw new InvalidOperationException("Failed to read seeded family members.");

        var annaId = family.Single(x => x.Name == "Anna").Id;
        var oskarId = family.Single(x => x.Name == "Oskar").Id;
        var today = DateOnly.FromDateTime(DateTime.Today).ToString("yyyy-MM-dd");
        var currentMonth = DateOnly.FromDateTime(DateTime.Today).ToString("yyyy-MM");

        await PostFormAsync(client, "/api/events", new Dictionary<string, string>
        {
            ["title"] = "Handballtrening",
            ["event_date"] = today,
            ["start_time"] = "17:00",
            ["end_time"] = "18:30",
            ["owner_id"] = annaId.ToString(),
            ["color"] = "#1d6b63",
            ["note"] = "Ta med drikkeflaske.",
        });

        await PostFormAsync(client, "/api/meals", new Dictionary<string, string>
        {
            ["day_of_week"] = ((((int)DateTime.Today.DayOfWeek) + 6) % 7).ToString(),
            ["meal_type"] = "dinner",
            ["meal"] = "Laks med ris",
            ["owner_id"] = oskarId.ToString(),
            ["note"] = "Laks, ris og agurk",
        });

        await PostJsonAsync(client, "/api/budget", new
        {
            set_budget = true,
            month = currentMonth,
            income = 62000,
            limit = 18000,
        });

        await PostJsonAsync(client, "/api/budget", new
        {
            amount = 1249,
            category = "Mat",
            expense_date = today,
            owner_id = annaId,
            description = "Ukens storhandel",
        });

        await PostFormAsync(client, "/api/notes", new Dictionary<string, string>
        {
            ["title"] = "Husk foreldremote",
            ["owner_id"] = oskarId.ToString(),
            ["content"] = "Sjekk tidspunkt og hvem som kan ga.",
        });

        await PostFormAsync(client, "/api/shopping", new Dictionary<string, string>
        {
            ["item"] = "Yoghurt",
            ["quantity"] = "4",
            ["owner_id"] = annaId.ToString(),
        });

        await PostFormAsync(client, "/api/family/assignments", new Dictionary<string, string>
        {
            ["day_of_week"] = "0",
            ["member_id"] = annaId.ToString(),
            ["activity_type"] = "activity",
            ["note"] = "Morgenrutine",
        });
    }

    private static async Task WaitForHealthyAsync()
    {
        using var client = new HttpClient { BaseAddress = new Uri(BaseUrl), Timeout = TimeSpan.FromSeconds(2) };

        for (var attempt = 0; attempt < 40; attempt += 1)
        {
            try
            {
                var response = await client.GetAsync("/health");
                if (response.IsSuccessStatusCode)
                {
                    return;
                }
            }
            catch
            {
            }

            await Task.Delay(500);
        }

        throw new TimeoutException($"Local app did not become healthy at {BaseUrl}.");
    }

    private static HttpClient CreateClient()
    {
        var handler = new HttpClientHandler
        {
            AllowAutoRedirect = false,
            UseCookies = true,
            CookieContainer = new CookieContainer(),
        };

        return new HttpClient(handler) { BaseAddress = new Uri(BaseUrl) };
    }

    private static async Task PostFormAsync(HttpClient client, string path, IDictionary<string, string> values)
    {
        var response = await client.PostAsync(path, new FormUrlEncodedContent(values));
        EnsureSuccessOrRedirect(response, path);
    }

    private static async Task PostJsonAsync(HttpClient client, string path, object payload)
    {
        var response = await client.PostAsJsonAsync(path, payload);
        EnsureSuccessOrRedirect(response, path);
    }

    private static void EnsureSuccessOrRedirect(HttpResponseMessage response, string operation)
    {
        if (response.IsSuccessStatusCode || response.StatusCode is HttpStatusCode.Found or HttpStatusCode.SeeOther)
        {
            return;
        }

        throw new InvalidOperationException($"Request for {operation} failed with status {(int)response.StatusCode}.");
    }

    private static async Task PumpStreamAsync(StreamReader reader, string path)
    {
        await using var writer = new StreamWriter(path, append: false);
        while (true)
        {
            var line = await reader.ReadLineAsync();
            if (line is null)
            {
                break;
            }

            await writer.WriteLineAsync(line);
            await writer.FlushAsync();
        }
    }

    private static int GetFreeTcpPort()
    {
        using var listener = new TcpListener(IPAddress.Loopback, 0);
        listener.Start();
        return ((IPEndPoint)listener.LocalEndpoint).Port;
    }

    private static string ResolveRepositoryRoot()
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);

        while (directory is not null)
        {
            if (File.Exists(Path.Combine(directory.FullName, "FamilyPlanner.csproj")))
            {
                return directory.FullName;
            }

            directory = directory.Parent;
        }

        throw new DirectoryNotFoundException("Could not locate repository root.");
    }
}
