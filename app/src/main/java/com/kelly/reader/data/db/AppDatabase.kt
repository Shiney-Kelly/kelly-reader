package com.kelly.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app database. One table so far: books. The schema is exported to app/schemas.
 */
@Database(entities = [BookEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {

        /**
         * Builds the database stored in the app's data folder.
         */
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "kelly-reader.db",
        ).build()
    }
}
