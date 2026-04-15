namespace FamilyPlanner.Services.Storage;

public sealed class StoragePaths
{
    public StoragePaths(IConfiguration configuration)
    {
        var configuredRoot = configuration["App:DataRoot"];
        var environmentRoot = Environment.GetEnvironmentVariable("FAMILYPLANNER_DATA_ROOT");

        RootPath = string.IsNullOrWhiteSpace(environmentRoot)
            ? string.IsNullOrWhiteSpace(configuredRoot)
                ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "FamilyPlanner")
                : configuredRoot
            : environmentRoot;
        UploadsPath = Path.Combine(RootPath, "uploads");
        AvatarsPath = Path.Combine(UploadsPath, "avatars");
        DatabasePath = Path.Combine(RootPath, "familyplanner.db");
    }

    public string RootPath { get; }
    public string UploadsPath { get; }
    public string AvatarsPath { get; }
    public string DatabasePath { get; }
}
