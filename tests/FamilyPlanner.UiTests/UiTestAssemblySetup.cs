using Microsoft.Playwright;
using NUnit.Framework;

namespace FamilyPlanner.UiTests;

[SetUpFixture]
public sealed class UiTestAssemblySetup
{
    [OneTimeSetUp]
    public async Task OneTimeSetUpAsync()
    {
        Environment.SetEnvironmentVariable("PLAYWRIGHT_BROWSERS_PATH", UiTestHost.BrowserPath);
        await UiTestHost.EnsureStartedAsync();
        Assertions.SetDefaultExpectTimeout(10_000);
    }

    [OneTimeTearDown]
    public async Task OneTimeTearDownAsync()
    {
        await UiTestHost.StopAsync();
    }
}
