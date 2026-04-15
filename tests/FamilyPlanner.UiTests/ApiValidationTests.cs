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
            new InvalidJsonCase("/api/medicines", """{"toggle":true}"""),
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
        var paths = new[] { "/api/budget", "/api/medicines", "/api/shopping" };
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

    private static StringContent JsonContent(string json) =>
        new(json, Encoding.UTF8, "application/json");

    private sealed record InvalidJsonCase(string Path, string Json);
}
