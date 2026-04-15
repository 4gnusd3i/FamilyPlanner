using Microsoft.AspNetCore.Http;

namespace FamilyPlanner.Services.Storage;

public sealed class AvatarStorageService(StoragePaths storagePaths)
{
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

    public async Task<string> SaveBytesAsync(
        byte[] bytes,
        string fileStem,
        string? extension,
        string? currentAvatarUrl,
        CancellationToken cancellationToken = default)
    {
        var safeExtension = NormalizeExtension(extension);
        var safeStem = string.Concat(fileStem.Where(ch => char.IsLetterOrDigit(ch) || ch is '-' or '_'));
        var fileName = $"{safeStem}{safeExtension}";
        var fullPath = Path.Combine(storagePaths.AvatarsPath, fileName);

        await File.WriteAllBytesAsync(fullPath, bytes, cancellationToken);
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

    private static string NormalizeExtension(string? extension)
    {
        if (string.IsNullOrWhiteSpace(extension))
        {
            return ".png";
        }

        return extension.StartsWith(".", StringComparison.Ordinal) ? extension.ToLowerInvariant() : $".{extension.ToLowerInvariant()}";
    }
}
