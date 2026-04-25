using System.Net;
using System.Text;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class ApiValidationTests
{
    [SetUp]
    public async Task SetUpAsync() => await UiTestHost.ResetStateAsync();

    [Test]
    public async Task JsonMutationCommands_ReturnBadRequestWhenRequiredIdsAreMissing()
    {
        var cases = new[]
        {
            new InvalidJsonCase("/api/events", """{"delete":true}"""),
            new InvalidJsonCase("/api/meals", """{"delete":true}"""),
            new InvalidJsonCase("/api/budget", """{"delete_expense":true}"""),
            new InvalidJsonCase("/api/notes", """{"delete":true}"""),
            new InvalidJsonCase("/api/shopping", """{"delete":true}""")
        };

        using var client = await UiTestHost.CreateClientAsync();

        foreach (var testCase in cases)
        {
            using var response = await client.PostAsync(testCase.Path, JsonContent(testCase.Json));
            var body = await response.Content.ReadAsStringAsync();

            Assert.Multiple(() =>
            {
                Assert.That(response.StatusCode, Is.EqualTo(HttpStatusCode.BadRequest), $"{testCase.Path} should reject missing IDs.");
                Assert.That(body, Does.Contain("error"), $"{testCase.Path} should return a stable error payload.");
            });
        }
    }

    [Test]
    public async Task JsonObjectEndpoints_ReturnBadRequestForNonObjectBodies()
    {
        var paths = new[] { "/api/budget", "/api/shopping" };
        using var client = await UiTestHost.CreateClientAsync();

        foreach (var path in paths)
        {
            using var response = await client.PostAsync(path, JsonContent("null"));
            var body = await response.Content.ReadAsStringAsync();

            Assert.Multiple(() =>
            {
                Assert.That(response.StatusCode, Is.EqualTo(HttpStatusCode.BadRequest), $"{path} should reject non-object JSON.");
                Assert.That(body, Does.Contain("error"), $"{path} should return a stable error payload.");
            });
        }
    }

    [Test]
    public async Task JsonMutationEndpoints_ReturnBadRequestForMalformedBodies()
    {
        var paths = new[] { "/api/events", "/api/meals", "/api/budget", "/api/notes", "/api/shopping" };
        using var client = await UiTestHost.CreateClientAsync();

        foreach (var path in paths)
        {
            using var response = await client.PostAsync(path, JsonContent("""{"delete":"""));
            var body = await response.Content.ReadAsStringAsync();

            Assert.Multiple(() =>
            {
                Assert.That(response.StatusCode, Is.EqualTo(HttpStatusCode.BadRequest), $"{path} should reject malformed JSON.");
                Assert.That(body, Does.Contain("error"), $"{path} should return a stable error payload.");
            });
        }
    }

    [Test]
    public async Task InvalidFormAndJsonFields_ReturnStableBadRequests()
    {
        var today = DateOnly.FromDateTime(DateTime.Today).ToString("yyyy-MM-dd");
        var cases = new InvalidRequestCase[]
        {
            new("/api/shopping", JsonContent("""{"item":"Melk","quantity":"mange"}"""), "shopping quantity"),
            new("/api/shopping", JsonContent("""{"item":"Melk","quantity":0}"""), "shopping quantity zero"),
            new("/api/budget", JsonContent("""{"amount":"mye"}"""), "budget amount"),
            new("/api/budget", JsonContent("""{"amount":0}"""), "budget amount zero"),
            new("/api/budget", JsonContent("""{"amount":10,"expense_date":"2026-99-99"}"""), "budget date"),
            new("/api/budget", JsonContent("""{"set_budget":true,"month":"2026-99","limit":100,"income":100}"""), "budget month"),
            new("/api/meals", FormContent(new Dictionary<string, string>
            {
                ["day_of_week"] = "7",
                ["meal"] = "Middag",
                ["meal_type"] = "dinner"
            }), "meal weekday"),
            new("/api/events", FormContent(new Dictionary<string, string>
            {
                ["title"] = "Ugyldig tid",
                ["event_date"] = today,
                ["start_time"] = "99:99"
            }), "event time")
        };

        using var client = await UiTestHost.CreateClientAsync();

        foreach (var testCase in cases)
        {
            using var response = await client.PostAsync(testCase.Path, testCase.Content);
            var body = await response.Content.ReadAsStringAsync();

            Assert.Multiple(() =>
            {
                Assert.That(response.StatusCode, Is.EqualTo(HttpStatusCode.BadRequest), $"{testCase.Name} should be rejected.");
                Assert.That(body, Does.Contain("error"), $"{testCase.Name} should return a stable error payload.");
            });
        }
    }

    [Test]
    public async Task ApiRequestsBeforeSetup_ReturnLocalizedSetupRequiredError()
    {
        await UiTestHost.ResetToUnconfiguredStateAsync();
        using var client = await UiTestHost.CreateClientAsync();

        using var response = await client.GetAsync($"/api/events?start={DateTime.Today:yyyy-MM-dd}&end={DateTime.Today:yyyy-MM-dd}");
        var body = await response.Content.ReadAsStringAsync();

        Assert.Multiple(() =>
        {
            Assert.That(response.StatusCode, Is.EqualTo(HttpStatusCode.Conflict));
            Assert.That(body, Does.Contain("Oppsett mangler."));
            Assert.That(body, Does.Not.Contain("setup_required"));
        });
    }

    private static StringContent JsonContent(string json) =>
        new(json, Encoding.UTF8, "application/json");

    private static FormUrlEncodedContent FormContent(IDictionary<string, string> values) => new(values);

    private sealed record InvalidJsonCase(string Path, string Json);

    private sealed record InvalidRequestCase(string Path, HttpContent Content, string Name);
}
