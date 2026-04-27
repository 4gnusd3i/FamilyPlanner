package io.github.fourgnusd3i.familyplanner.core.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.fourgnusd3i.familyplanner.core.time.DateTimeProvider
import io.github.fourgnusd3i.familyplanner.data.local.FamilyPlannerDatabase
import io.github.fourgnusd3i.familyplanner.data.local.PlannerDao
import io.github.fourgnusd3i.familyplanner.data.repository.PlannerRepository
import io.github.fourgnusd3i.familyplanner.data.repository.RoomPlannerRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamilyPlannerDatabase =
        Room.databaseBuilder(
            context,
            FamilyPlannerDatabase::class.java,
            "familyplanner.db",
        ).build()

    @Provides
    fun providePlannerDao(database: FamilyPlannerDatabase): PlannerDao = database.plannerDao()

    @Provides
    fun provideDateTimeProvider(): DateTimeProvider = DateTimeProvider.System
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPlannerRepository(repository: RoomPlannerRepository): PlannerRepository
}
