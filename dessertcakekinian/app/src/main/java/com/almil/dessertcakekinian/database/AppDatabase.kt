package com.almil.dessertcakekinian.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProdukKategoriEntity::class,
        DetailStokEntity::class,
        HargaGrosirEntity::class,
        OrderEntity::class,
        DetailOrderEntity::class
    ],
    version = 3, // Update versi database dari 2 ke 3
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun produkDao(): ProdukDao
    abstract fun detailStokDao(): DetailStokDao
    abstract fun hargaGrosirDao(): HargaGrosirDao
    abstract fun orderDao(): OrderDao
    abstract fun detailOrderDao(): DetailOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dessert_cake_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}