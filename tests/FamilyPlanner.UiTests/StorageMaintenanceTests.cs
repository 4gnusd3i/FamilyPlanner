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
    public void ResetForFreshSetup_DropsObsoleteCollectionsAndClearsFeatureData()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            using (var store = CreateStore(dataRoot))
            {
                store.InitializeHousehold("Testfamilien");
                var member = store.UpsertFamilyMember(null, "Anna", "2015-04-15", null, "#a2d2ff", null);
                store.UpsertEvent(null, "Avtale", "2026-04-25", null, null, null, null, member.Id, null);
                store.UpsertMeal(null, 0, "dinner", "Middag", member.Id, null);
                store.SetBudget("2026-04", 1000, 2000);
                store.AddExpense(100, "Mat", "2026-04-25", member.Id, "Test");
                store.UpsertNote(null, "Notat", member.Id, "Innhold");
                store.UpsertShoppingItem(null, "Melk", 1, member.Id);
            }

            using (var database = new LiteDatabase(Path.Combine(dataRoot, "familyplanner.db")))
            {
                database.GetCollection("users").Insert(new BsonDocument { ["email"] = "legacy@example.test" });
                database.GetCollection("medicines").Insert(new BsonDocument { ["name"] = "Legacy medicine" });
                database.GetCollection("familyAssignments").Insert(new BsonDocument { ["note"] = "Legacy assignment" });
            }

            using (var store = CreateStore(dataRoot))
            {
                store.ResetForFreshSetup();

                Assert.Multiple(() =>
                {
                    Assert.That(store.HasHouseholdProfile(), Is.False);
                    Assert.That(store.GetFamilyMembers(), Is.Empty);
                    Assert.That(store.GetEvents(new DateOnly(2026, 4, 25), new DateOnly(2026, 4, 25)), Is.Empty);
                    Assert.That(store.GetMeals(), Is.Empty);
                    Assert.That(store.GetBudgetSnapshot("2026-04").Expenses, Is.Empty);
                    Assert.That(store.GetNotes(), Is.Empty);
                    Assert.That(store.GetShoppingItems(), Is.Empty);
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
    public void GetEvents_CreatesLeapDayBirthdaysOnValidCalendarDay()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            using var store = CreateStore(dataRoot);
            store.InitializeHousehold("Testfamilien");
            store.UpsertFamilyMember(null, "Leap", "2016-02-29", null, "#a2d2ff", null);

            var nonLeapBirthday = store.GetEvents(new DateOnly(2027, 2, 28), new DateOnly(2027, 2, 28))
                .Single(x => x.SourceType == "birthday");
            var leapBirthday = store.GetEvents(new DateOnly(2028, 2, 29), new DateOnly(2028, 2, 29))
                .Single(x => x.SourceType == "birthday");

            Assert.Multiple(() =>
            {
                Assert.That(nonLeapBirthday.EventDate, Is.EqualTo("2027-02-28"));
                Assert.That(nonLeapBirthday.Title, Is.EqualTo("Leap har bursdag"));
                Assert.That(leapBirthday.EventDate, Is.EqualTo("2028-02-29"));
                Assert.That(leapBirthday.Title, Is.EqualTo("Leap har bursdag"));
            });
        }
        finally
        {
            DeleteDirectory(dataRoot);
        }
    }

    [Test]
    public void DeleteFamilyMember_RemovesBirthdayEventsAndClearsOwnerReferences()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            using var store = CreateStore(dataRoot);
            store.InitializeHousehold("Testfamilien");
            var member = store.UpsertFamilyMember(null, "Anna", "2015-04-15", null, "#a2d2ff", null);
            store.GetEvents(new DateOnly(2026, 4, 15), new DateOnly(2026, 4, 15));
            store.UpsertEvent(null, "Avtale", "2026-04-25", null, null, null, null, member.Id, null);
            store.UpsertMeal(null, 0, "dinner", "Middag", member.Id, null);
            store.SetBudget("2026-04", 1000, 2000);
            store.AddExpense(100, "Mat", "2026-04-25", member.Id, "Test");
            store.UpsertNote(null, "Notat", member.Id, "Innhold");
            store.UpsertShoppingItem(null, "Melk", 1, member.Id);

            store.DeleteFamilyMember(member.Id);

            Assert.Multiple(() =>
            {
                Assert.That(store.GetEvents(new DateOnly(2026, 4, 15), new DateOnly(2026, 4, 15)).Any(x => x.SourceType == "birthday"), Is.False);
                Assert.That(store.GetEvents(new DateOnly(2026, 4, 25), new DateOnly(2026, 4, 25)).Single(x => x.Title == "Avtale").OwnerId, Is.Null);
                Assert.That(store.GetMeals().Single(x => x.Meal == "Middag").OwnerId, Is.Null);
                Assert.That(store.GetBudgetSnapshot("2026-04").Expenses.Single().OwnerId, Is.Null);
                Assert.That(store.GetNotes().Single(x => x.Title == "Notat").OwnerId, Is.Null);
                Assert.That(store.GetShoppingItems().Single(x => x.Item == "Melk").OwnerId, Is.Null);
            });
        }
        finally
        {
            DeleteDirectory(dataRoot);
        }
    }

    [Test]
    public void UpsertFamilyMember_NormalizesColorInputAtStorageBoundary()
    {
        var dataRoot = CreateTempDataRoot();

        try
        {
            using var store = CreateStore(dataRoot);
            store.InitializeHousehold("Testfamilien");

            var normalized = store.UpsertFamilyMember(null, "Anna", null, null, "#ABCDEF", null);
            var fallback = store.UpsertFamilyMember(null, "Oskar", null, null, "red; background:url(test)", null);

            Assert.Multiple(() =>
            {
                Assert.That(normalized.Color, Is.EqualTo("#abcdef"));
                Assert.That(fallback.Color, Is.EqualTo("#3b82f6"));
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

            Assert.ThrowsAsync<InvalidAvatarFormatException>(() => avatarStorage.SaveUploadedAsync(file, null));
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
