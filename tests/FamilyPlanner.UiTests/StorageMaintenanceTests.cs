using FamilyPlanner.Models;
using FamilyPlanner.Services.Storage;
using LiteDB;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Configuration;

namespace FamilyPlanner.UiTests;

[TestFixture]
public sealed class StorageMaintenanceTests
{
    [Test]
    public void RunMaintenance_DropsObsoleteCollectionsAndPurgesExpiredItems()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            using (var store = CreateStore(dataRoot))
            {
                store.InitializeHousehold("Testfamilien");

                var shoppingItem = store.UpsertShoppingItem(null, "Ferdig vare", 1, null);
                store.ToggleShoppingItem(shoppingItem.Id);
            }

            using (var database = new LiteDatabase(Path.Combine(dataRoot, "familyplanner.db")))
            {
                database.GetCollection("users").Insert(new BsonDocument { ["email"] = "legacy@example.test" });
                database.GetCollection("medicines").Insert(new BsonDocument { ["name"] = "Legacy medicine" });
                database.GetCollection("familyAssignments").Insert(new BsonDocument { ["note"] = "Legacy assignment" });

                var shoppingItems = database.GetCollection<ShoppingItem>("shoppingItems");
                var doneItem = shoppingItems.FindOne(x => x.Item == "Ferdig vare");
                doneItem.DoneAt = DateTime.Now.AddSeconds(-20);
                shoppingItems.Update(doneItem);
            }

            using (var store = CreateStore(dataRoot))
            {
                store.RunMaintenance();

                Assert.Multiple(() =>
                {
                    Assert.That(store.GetShoppingItems().Any(x => x.Item == "Ferdig vare"), Is.False);
                });
            }

            using (var database = new LiteDatabase(Path.Combine(dataRoot, "familyplanner.db")))
            {
                Assert.Multiple(() =>
                {
                    Assert.That(database.GetCollectionNames(), Does.Not.Contain("users"));
                    Assert.That(database.GetCollectionNames(), Does.Not.Contain("medicines"));
                    Assert.That(database.GetCollectionNames(), Does.Not.Contain("familyAssignments"));
                });
            }
        }
        finally
        {
            DeleteDirectory(dataRoot);
        }
    }

    [Test]
    public void ResetForFreshSetup_AndAvatarCleanup_ClearLocalHouseholdData()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            using var store = CreateStore(dataRoot);
            var avatarStorage = new AvatarStorageService(CreateStoragePaths(dataRoot));
            Directory.CreateDirectory(Path.Combine(dataRoot, "uploads", "avatars"));
            File.WriteAllText(Path.Combine(dataRoot, "uploads", "avatars", "stale.png"), "old avatar");

            store.InitializeHousehold("Testfamilien");
            store.UpsertFamilyMember(null, "Anna", "2015-04-15", null, "#a2d2ff", "/uploads/avatars/stale.png");
            store.ResetForFreshSetup();
            avatarStorage.DeleteAllLocalAvatars();

            Assert.Multiple(() =>
            {
                Assert.That(store.HasHouseholdProfile(), Is.False);
                Assert.That(store.GetFamilyMembers(), Is.Empty);
                Assert.That(Directory.EnumerateFiles(Path.Combine(dataRoot, "uploads", "avatars")), Is.Empty);
            });
        }
        finally
        {
            DeleteDirectory(dataRoot);
        }
    }

    [Test]
    public void AvatarStorage_RejectsUnsupportedFileExtensions()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            var avatarStorage = new AvatarStorageService(CreateStoragePaths(dataRoot));
            using var stream = new MemoryStream([1, 2, 3]);
            var file = new FormFile(stream, 0, stream.Length, "avatar", "avatar.svg");

            Assert.ThrowsAsync<BadHttpRequestException>(() => avatarStorage.SaveUploadedAsync(file, null));
        }
        finally
        {
            DeleteDirectory(dataRoot);
        }
    }

    private static PlannerStore CreateStore(string dataRoot) => new(CreateStoragePaths(dataRoot));

    private static StoragePaths CreateStoragePaths(string dataRoot)
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["App:DataRoot"] = dataRoot
            })
            .Build();

        var storagePaths = new StoragePaths(configuration);
        Directory.CreateDirectory(storagePaths.RootPath);
        Directory.CreateDirectory(storagePaths.UploadsPath);
        Directory.CreateDirectory(storagePaths.AvatarsPath);
        return storagePaths;
    }

    private static string CreateTempDataRoot() =>
        Path.Combine(Path.GetTempPath(), $"familyplanner-storage-tests-{Guid.NewGuid():N}");

    private static void DeleteDirectory(string path)
    {
        if (Directory.Exists(path))
        {
            Directory.Delete(path, recursive: true);
        }
    }
}
