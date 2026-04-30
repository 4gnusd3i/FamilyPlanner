package io.github.by4gnusd3i.familyplanner.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PlannerDaoInstrumentedTest {
    private lateinit var database: FamilyPlannerDatabase
    private lateinit var dao: PlannerDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FamilyPlannerDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.plannerDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun resetAllClearsPlannerCollections() = runBlocking {
        dao.initializeHousehold(
            profile = HouseholdProfileEntity(familyName = "Hansen"),
            firstMember = FamilyMemberEntity(name = "Anna"),
        )
        dao.upsertNote(NoteEntity(title = "Pakkliste"))
        dao.upsertShoppingItem(ShoppingItemEntity(item = "Melk"))

        dao.resetAll()

        assertEquals(0, dao.observeHouseholdCount().first())
        assertEquals(emptyList<FamilyMemberEntity>(), dao.observeFamilyMembers().first())
        assertEquals(emptyList<NoteEntity>(), dao.observeNotes().first())
        assertEquals(emptyList<ShoppingItemEntity>(), dao.observeShoppingItems().first())
    }

    @Test
    fun deletingFamilyMemberDetachesOwnedPlannerItems() = runBlocking {
        val memberId = dao.upsertFamilyMember(FamilyMemberEntity(name = "Liv", color = "#10b981"))
        val eventId = dao.upsertEvent(
            PlannerEventEntity(
                title = "Lesing",
                eventDate = LocalDate.of(2026, 4, 22),
                ownerId = memberId,
                color = "#10b981",
            ),
        )
        val mealId = dao.upsertMeal(
            MealPlanEntity(dayOfWeek = 2, mealType = "dinner", meal = "Taco", ownerId = memberId),
        )
        val expenseId = dao.upsertExpense(
            ExpenseEntity(
                amount = BigDecimal("149.90"),
                category = "Mat",
                expenseDate = LocalDate.of(2026, 4, 22),
                ownerId = memberId,
                month = "2026-04",
            ),
        )
        val noteId = dao.upsertNote(NoteEntity(title = "Notat", ownerId = memberId))
        val shoppingId = dao.upsertShoppingItem(ShoppingItemEntity(item = "Melk", ownerId = memberId))

        dao.deleteFamilyMemberAndDetachReferences(memberId, neutralEventColor = "#eaf4ff")

        assertNull(dao.getFamilyMemberById(memberId))
        assertNull(dao.getEventById(eventId)?.ownerId)
        assertEquals("#eaf4ff", dao.getEventById(eventId)?.color)
        assertNull(dao.getMealById(mealId)?.ownerId)
        assertNull(dao.getExpenseById(expenseId)?.ownerId)
        assertNull(dao.getNoteById(noteId)?.ownerId)
        assertNull(dao.getShoppingItemById(shoppingId)?.ownerId)
    }
}
