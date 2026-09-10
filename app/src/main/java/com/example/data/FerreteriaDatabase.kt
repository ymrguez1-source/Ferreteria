package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        VentaEntity::class,
        MermaEntity::class,
        MovimientoEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FerreteriaDatabase : RoomDatabase() {
    abstract fun ferreteriaDao(): FerreteriaDao

    companion object {
        @Volatile
        private var INSTANCE: FerreteriaDatabase? = null

        fun getDatabase(context: Context): FerreteriaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FerreteriaDatabase::class.java,
                    "ferreteria_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
