package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val costoUnitario: Double,
    val precioVenta: Double,
    val unidadMedida: String,
    val stock: Double,
    val fechaCreacion: Long = System.currentTimeMillis()
)

@Entity(tableName = "ventas")
data class VentaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productoId: Long,
    val productoNombre: String,
    val cantidad: Double,
    val precioUnitario: Double,
    val total: Double,
    val ganancia: Double,
    val cliente: String = "",
    val fecha: Long = System.currentTimeMillis()
)

@Entity(tableName = "mermas")
data class MermaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productoId: Long,
    val productoNombre: String,
    val cantidad: Double,
    val razon: String,
    val costoPerdida: Double,
    val fecha: Long = System.currentTimeMillis()
)

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productoId: Long,
    val productoNombre: String,
    val tipo: String, // "venta", "merma", "ajuste"
    val cantidad: Double,
    val valorUnitario: Double,
    val total: Double,
    val referencia: String,
    val fecha: Long = System.currentTimeMillis()
)

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: String, // "administrador", "operador"
    val fechaCreacion: Long = System.currentTimeMillis()
)
