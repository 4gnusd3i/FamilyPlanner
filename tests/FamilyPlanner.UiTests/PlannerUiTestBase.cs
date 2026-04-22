using System.Text.RegularExpressions;
using System.Net.Http.Json;
using Microsoft.Playwright;
using Microsoft.Playwright.NUnit;
using NUnit.Framework.Interfaces;
using static Microsoft.Playwright.Assertions;

namespace FamilyPlanner.UiTests;

[Parallelizable(ParallelScope.Self)]
public abstract class PlannerUiTestBase : PageTest
{
    private bool _tracingStarted;

    protected abstract int ViewportWidth { get; }
    protected abstract int ViewportHeight { get; }

    public override BrowserNewContextOptions ContextOptions() =>
        new()
        {
            BaseURL = UiTestHost.BaseUrl,
            ColorScheme = ColorScheme.Light,
            Locale = "nb-NO",
            HasTouch = true,
            ViewportSize = new ViewportSize
            {
                Width = ViewportWidth,
                Height = ViewportHeight,
            }
        };

    [SetUp]
    public async Task SetUpAsync()
    {
        await UiTestHost.ResetStateAsync();
        await Context.ClearCookiesAsync();

        await Context.Tracing.StartAsync(new TracingStartOptions
        {
            Screenshots = true,
            Snapshots = true,
            Sources = true,
        });
        _tracingStarted = true;

        await Page.GotoAsync("/");
        await Expect(Page.Locator(".quick-actions")).ToBeVisibleAsync();
        await AssertNoHorizontalOverflowAsync();
    }

    [TearDown]
    public async Task TearDownAsync()
    {
        if (!_tracingStarted)
        {
            return;
        }

        var testName = SanitizeFileName(TestContext.CurrentContext.Test.FullName ?? TestContext.CurrentContext.Test.Name);
        var tracePath = Path.Combine(UiTestHost.ArtifactsRoot, $"{testName}.trace.zip");

        await Context.Tracing.StopAsync(new TracingStopOptions
        {
            Path = tracePath,
        });

        if (TestContext.CurrentContext.Result.Outcome.Status is not TestStatus.Passed)
        {
            await Page.ScreenshotAsync(new PageScreenshotOptions
            {
                Path = Path.Combine(UiTestHost.ArtifactsRoot, $"{testName}.png"),
                FullPage = true,
            });
        }

        _tracingStarted = false;
    }

    protected Task<int> GetGridColumnCountAsync(string selector) =>
        Page.Locator(selector).EvaluateAsync<int>(
            @"element => {
                const columns = getComputedStyle(element).gridTemplateColumns;
                if (!columns || columns === 'none') return 0;
                return columns.split(' ').filter(Boolean).length;
            }");

    protected async Task AssertNoHorizontalOverflowAsync()
    {
        var hasOverflow = await Page.EvaluateAsync<bool>(
            "() => document.documentElement.scrollWidth > window.innerWidth + 1");

        Assert.That(hasOverflow, Is.False, "The current viewport has horizontal overflow.");
    }

    protected async Task AssertLocatorFitsViewportWidthAsync(string selector, double padding = 4d)
    {
        var locator = Page.Locator(selector).First;
        await Expect(locator).ToBeVisibleAsync();
        await locator.ScrollIntoViewIfNeededAsync();

        var rect = await GetClientRectAsync(locator);
        Assert.Multiple(() =>
        {
            Assert.That(rect[0], Is.GreaterThanOrEqualTo(-padding), $"{selector} extends past the left viewport edge.");
            Assert.That(rect[2], Is.LessThanOrEqualTo(ViewportWidth + padding), $"{selector} extends past the right viewport edge.");
            Assert.That(rect[4], Is.GreaterThan(0d), $"{selector} has zero width.");
        });
    }

    protected async Task AssertLocatorFullyWithinViewportAsync(string selector, double padding = 4d)
    {
        var locator = Page.Locator(selector).First;
        await Expect(locator).ToBeVisibleAsync();
        await locator.ScrollIntoViewIfNeededAsync();

        var rect = await GetClientRectAsync(locator);
        Assert.Multiple(() =>
        {
            Assert.That(rect[0], Is.GreaterThanOrEqualTo(-padding), $"{selector} extends past the left viewport edge.");
            Assert.That(rect[1], Is.GreaterThanOrEqualTo(-padding), $"{selector} extends past the top viewport edge.");
            Assert.That(rect[2], Is.LessThanOrEqualTo(ViewportWidth + padding), $"{selector} extends past the right viewport edge.");
            Assert.That(rect[3], Is.LessThanOrEqualTo(ViewportHeight + padding), $"{selector} extends past the bottom viewport edge.");
        });
    }

    protected async Task AssertLocatorsDoNotOverlapAsync(string firstSelector, string secondSelector)
    {
        var first = Page.Locator(firstSelector).First;
        var second = Page.Locator(secondSelector).First;

        await Expect(first).ToBeVisibleAsync();
        await Expect(second).ToBeVisibleAsync();

        var firstRect = await GetClientRectAsync(first);
        var secondRect = await GetClientRectAsync(second);
        var overlaps = !(firstRect[2] <= secondRect[0] + 1d ||
                         firstRect[0] >= secondRect[2] - 1d ||
                         firstRect[3] <= secondRect[1] + 1d ||
                         firstRect[1] >= secondRect[3] - 1d);

        Assert.That(overlaps, Is.False, $"{firstSelector} overlaps {secondSelector}.");
    }

    protected async Task AssertLocatorsStackVerticallyAsync(params string[] selectors)
    {
        double? previousBottom = null;

        foreach (var selector in selectors)
        {
            var locator = Page.Locator(selector).First;
            await Expect(locator).ToBeVisibleAsync();
            await locator.ScrollIntoViewIfNeededAsync();

            var rect = await GetClientRectAsync(locator);
            if (previousBottom.HasValue)
            {
                Assert.That(rect[1], Is.GreaterThanOrEqualTo(previousBottom.Value - 1d), $"{selector} overlaps the previous stacked element.");
            }

            previousBottom = rect[3];
        }
    }

    protected async Task AssertMinimumSizeAsync(string selector, double minWidth, double minHeight) =>
        await AssertMinimumSizeAsync(Page.Locator(selector).First, selector, minWidth, minHeight);

    protected async Task AssertAllMinimumSizeAsync(string selector, double minWidth, double minHeight)
    {
        var locator = Page.Locator(selector);
        var count = await locator.CountAsync();
        Assert.That(count, Is.GreaterThan(0), $"No elements matched {selector}.");

        for (var index = 0; index < count; index += 1)
        {
            await AssertMinimumSizeAsync(locator.Nth(index), $"{selector} [{index}]", minWidth, minHeight);
        }
    }

    protected async Task WaitForTextChangeAsync(string selector, string previousValue) =>
        await Page.WaitForFunctionAsync(
            @"([cssSelector, currentValue]) => {
                const node = document.querySelector(cssSelector);
                return !!node && node.textContent.trim() !== currentValue;
            }",
            new object[] { selector, previousValue });

    protected async Task WaitForExactTextAsync(string selector, string expectedValue) =>
        await Page.WaitForFunctionAsync(
            @"([cssSelector, expectedText]) => {
                const node = document.querySelector(cssSelector);
                return !!node && node.textContent.trim() === expectedText;
            }",
            new object[] { selector, expectedValue });

    protected async Task OpenModalAsync(string buttonName, string modalId)
    {
        await Page.GetByRole(AriaRole.Button, new() { Name = buttonName, Exact = true }).ClickAsync();
        await WaitForModalStateAsync(modalId, open: true);
    }

    protected async Task OpenModalBySelectorAsync(string selector, string modalId)
    {
        await Page.Locator(selector).First.ClickAsync();
        await WaitForModalStateAsync(modalId, open: true);
    }

    protected async Task DragFamilyMemberToDayAsync(string memberName, int dayIndex)
    {
        await Page.EvaluateAsync(
            @"([name, day]) => {
                const source = Array.from(document.querySelectorAll('.family-avatar'))
                    .find((element) => element.textContent && element.textContent.includes(name));
                const target = document.querySelector(`.day-box[data-day='${day}']`);
                if (!source || !target) {
                    throw new Error('Could not locate drag source or target.');
                }

                const dataTransfer = new DataTransfer();
                source.dispatchEvent(new DragEvent('dragstart', { bubbles: true, cancelable: true, dataTransfer }));
                target.dispatchEvent(new DragEvent('dragenter', { bubbles: true, cancelable: true, dataTransfer }));
                target.dispatchEvent(new DragEvent('dragover', { bubbles: true, cancelable: true, dataTransfer }));
                target.dispatchEvent(new DragEvent('drop', { bubbles: true, cancelable: true, dataTransfer }));
                source.dispatchEvent(new DragEvent('dragend', { bubbles: true, cancelable: true, dataTransfer }));
            }",
            new object[] { memberName, dayIndex });
    }

    protected async Task LongPressFamilyMemberToDayAsync(string memberName, int dayIndex)
    {
        var diagnostic = await Page.EvaluateAsync<string>(
            @"async ([name, day]) => {
                const source = Array.from(document.querySelectorAll('.family-avatar'))
                    .find((element) => element.textContent && element.textContent.includes(name));
                const target = document.querySelector(`.day-box[data-day='${day}']`);
                if (!source || !target) {
                    throw new Error('Could not locate touch drag source or target.');
                }

                const sourceRect = source.getBoundingClientRect();
                const targetRect = target.getBoundingClientRect();
                const touchId = 701;
                const startX = sourceRect.left + (sourceRect.width / 2);
                const startY = sourceRect.top + (sourceRect.height / 2);
                const endX = targetRect.left + (targetRect.width / 2);
                const endY = targetRect.top + Math.min(targetRect.height - 10, 80);
                const createTouch = (targetElement, x, y) => new Touch({
                    identifier: touchId,
                    target: targetElement,
                    clientX: x,
                    clientY: y,
                    screenX: x,
                    screenY: y,
                    radiusX: 4,
                    radiusY: 4,
                    force: 1
                });

                const startTouch = createTouch(source, startX, startY);
                source.dispatchEvent(new TouchEvent('touchstart', {
                    bubbles: true,
                    cancelable: true,
                    touches: [startTouch],
                    targetTouches: [startTouch],
                    changedTouches: [startTouch]
                }));

                await new Promise((resolve) => setTimeout(resolve, 430));

                const moveTouch = createTouch(target, endX, endY);
                document.dispatchEvent(new TouchEvent('touchmove', {
                    bubbles: true,
                    cancelable: true,
                    touches: [moveTouch],
                    targetTouches: [moveTouch],
                    changedTouches: [moveTouch]
                }));
                document.dispatchEvent(new TouchEvent('touchend', {
                    bubbles: true,
                    cancelable: true,
                    touches: [],
                    targetTouches: [],
                    changedTouches: [moveTouch]
                }));

                return JSON.stringify({
                    modalOpen: document.getElementById('eventModal')?.classList.contains('active') === true,
                    dragBound: source.dataset.dragBound || '',
                    longPressActive: source.dataset.longPressActive || '',
                    bodyDragging: document.body.classList.contains('is-dragging-family'),
                    endpointClass: document.elementFromPoint(endX, endY)?.className || '',
                    endpointDay: document.elementFromPoint(endX, endY)?.closest('.day-box')?.dataset.date || '',
                    touchEventType: typeof TouchEvent,
                    touchType: typeof Touch
                });
            }",
            new object[] { memberName, dayIndex });

        if (!diagnostic.Contains("\"modalOpen\":true", StringComparison.Ordinal))
        {
            throw new AssertionException($"Long-press touch drag did not open the event modal. Diagnostic: {diagnostic}");
        }
    }

    protected async Task SelectCustomOptionAsync(string selectId, string value)
    {
        var root = Page.Locator($".custom-select[data-select-id='{selectId}']");
        await Expect(root).ToBeVisibleAsync();
        await root.Locator(".custom-select-trigger").ClickAsync();
        var option = root.Locator($".custom-select-option[data-value='{value}']");
        await Expect(option).ToBeVisibleAsync();
        await option.ClickAsync();
        await Expect(Page.Locator($"#{selectId}")).ToHaveValueAsync(value);
    }

    protected async Task AssertModalFitsViewportAsync(string modalId)
    {
        var width = await Page.Locator($"#{modalId} .modal").EvaluateAsync<double>("el => el.getBoundingClientRect().width");
        Assert.That(width, Is.LessThanOrEqualTo(ViewportWidth - 12d));
    }

    protected async Task WaitForModalStateAsync(string modalId, bool open)
    {
        await Page.WaitForFunctionAsync(
            @"([modal, expected]) => document.getElementById(modal)?.classList.contains('active') === expected",
            new object[] { modalId, open });
    }

    protected async Task AcceptDialogAsync(Func<Task> trigger, string? expectedFragment = null)
    {
        var completion = new TaskCompletionSource<IDialog>(TaskCreationOptions.RunContinuationsAsynchronously);

        void HandleDialog(object? _, IDialog dialog) => completion.TrySetResult(dialog);

        Page.Dialog += HandleDialog;
        try
        {
            var triggerTask = trigger();
            var dialog = await completion.Task.WaitAsync(TimeSpan.FromSeconds(5));
            if (!string.IsNullOrWhiteSpace(expectedFragment))
            {
                Assert.That(dialog.Message, Does.Contain(expectedFragment));
            }

            await dialog.AcceptAsync();
            await triggerTask;
        }
        finally
        {
            Page.Dialog -= HandleDialog;
        }
    }

    protected async Task<T?> GetApiAsync<T>(string path)
    {
        using var client = await UiTestHost.CreateClientAsync();
        return await client.GetFromJsonAsync<T>(path);
    }

    protected async Task PostFormAsync(string path, IDictionary<string, string> values)
    {
        using var client = await UiTestHost.CreateClientAsync();
        var response = await client.PostAsync(path, new FormUrlEncodedContent(values));
        response.EnsureSuccessStatusCode();
    }

    protected async Task PostJsonAsync(string path, object payload)
    {
        using var client = await UiTestHost.CreateClientAsync();
        var response = await client.PostAsJsonAsync(path, payload);
        response.EnsureSuccessStatusCode();
    }

    protected async Task WaitForToastAsync(string expectedMessage) =>
        await Expect(Page.Locator("#statusBanner")).ToContainTextAsync(expectedMessage);

    protected ILocator ModalLocator(string modalId) => Page.Locator($"#{modalId}");

    private static async Task<double[]> GetClientRectAsync(ILocator locator) =>
        await locator.EvaluateAsync<double[]>(
            @"element => {
                const rect = element.getBoundingClientRect();
                return [rect.left, rect.top, rect.right, rect.bottom, rect.width, rect.height];
            }");

    private async Task AssertMinimumSizeAsync(ILocator locator, string description, double minWidth, double minHeight)
    {
        await Expect(locator).ToBeVisibleAsync();
        var rect = await GetClientRectAsync(locator);

        Assert.Multiple(() =>
        {
            Assert.That(rect[4], Is.GreaterThanOrEqualTo(minWidth), $"{description} is narrower than expected.");
            Assert.That(rect[5], Is.GreaterThanOrEqualTo(minHeight), $"{description} is shorter than expected.");
        });
    }

    private static string SanitizeFileName(string value) =>
        Regex.Replace(value, @"[^A-Za-z0-9._-]", "_");
}
