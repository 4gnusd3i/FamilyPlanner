using Microsoft.AspNetCore.Http;

namespace FamilyPlanner.Services.Storage;

public sealed class AvatarStorageService(StoragePaths storagePaths)
{
    private static readonly HashSet<string> AllowedExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        ".gif",
        ".jpg",
        ".jpeg",
        ".png",
        ".webp"
    };

    public async Task<string> SaveUploadedAsync(IFormFile file, string? currentAvatarUrl, CancellationToken cancellationToken = default)
    {
        var extension = NormalizeExtension(Path.GetExtension(file.FileName));
        var fileName = $"avatar_{Guid.NewGuid():N}{extension}";
        var fullPath = Path.Combine(storagePaths.AvatarsPath, fileName);

        await using var stream = File.Create(fullPath);
        await file.CopyToAsync(stream, cancellationToken);

        DeleteIfLocal(currentAvatarUrl);
        return $"/uploads/avatars/{fileName}";
    }

    public void DeleteIfLocal(string? avatarUrl)
    {
        if (string.IsNullOrWhiteSpace(avatarUrl) || !avatarUrl.StartsWith("/uploads/avatars/", StringComparison.OrdinalIgnoreCase))
        {
            return;
        }

        var fileName = Path.GetFileName(avatarUrl);
        var fullPath = Path.Combine(storagePaths.AvatarsPath, fileName);
        if (File.Exists(fullPath))
        {
            File.Delete(fullPath);
        }
    }

    public void DeleteAllLocalAvatars()
    {
        if (!Directory.Exists(storagePaths.AvatarsPath))
        {
            return;
        }

        foreach (var filePath in Directory.EnumerateFiles(storagePaths.AvatarsPath))
        {
            File.Delete(filePath);
        }
    }

    private static string NormalizeExtension(string? extension)
    {
        if (string.IsNullOrWhiteSpace(extension))
        {
            return ".png";
        }

        var normalized = extension.StartsWith(".", StringComparison.Ordinal)
            ? extension.ToLowerInvariant()
            : $".{extension.ToLowerInvariant()}";

        if (!AllowedExtensions.Contains(normalized))
        {
            throw new BadHttpRequestException("Ugyldig avatarformat.");
        }

        return normalized;
    }
}
