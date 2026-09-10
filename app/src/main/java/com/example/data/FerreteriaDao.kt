package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FerreteriaDao {

    // --- PRODUCTOS ---
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun getProductByIdSync(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE productos SET stock = :newStock WHERE id = :id")
    suspend fun updateProductStock(id: Long, newStock: Double)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM productos")
    suspend fun deleteAllProducts()

    // --- VENTAS ---
    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAllVentas(): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas WHERE id = :id LIMIT 1")
    suspend fun getVentaById(id: Long): VentaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenta(venta: VentaEntity): Long

    @Query("DELETE FROM ventas WHERE id = :id")
    suspend fun deleteVentaById(id: Long)

    @Query("DELETE FROM ventas WHERE productoId = :productoId")
    suspend fun deleteVentasByProductId(productoId: Long)

    @Query("DELETE FROM ventas")
    suspend fun deleteAllVentas()

    // --- MERMAS ---
    @Query("SELECT * FROM mermas ORDER BY fecha DESC")
    fun getAllMermas(): Flow<List<MermaEntity>>

    @Query("SELECT * FROM mermas WHERE id = :id LIMIT 1")
    suspend fun getMermaById(id: Long): MermaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerma(merma: MermaEntity): Long

    @Query("DELETE FROM mermas WHERE id = :id")
    suspend fun deleteMermaById(id: Long)

    @Query("DELETE FROM mermas WHERE productoId = :productoId")
    suspend fun deleteMermasByProductId(productoId: Long)

    @Query("DELETE FROM mermas")
    suspend fun deleteAllMermas()

    // --- MOVIMIENTOS ---
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun getAllMovimientos(): Flow<List<MovimientoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovimiento(movimiento: MovimientoEntity): Long

    @Query("DELETE FROM movimientos WHERE referencia = :referencia")
    suspend fun deleteMovimientosByReferencia(referencia: String)

    @Query("DELETE FROM movimientos WHERE productoId = :productoId")
    suspend fun deleteMovimientosByProductId(productoId: Long)

    @Query("DELETE FROM movimientos")
    suspend fun deleteAllMovimientos()

    // --- USUARIOS ---
    @Query("SELECT * FROM usuarios ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM usuarios WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("DELETE FROM usuarios WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun countUsers(): Int
}
