package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.random.Random

class FerreteriaRepository(private val dao: FerreteriaDao) {

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allVentas: Flow<List<VentaEntity>> = dao.getAllVentas()
    val allMermas: Flow<List<MermaEntity>> = dao.getAllMermas()
    val allMovimientos: Flow<List<MovimientoEntity>> = dao.getAllMovimientos()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()

    suspend fun saveProduct(
        id: Long = 0,
        nombre: String,
        categoria: String,
        costoUnitario: Double,
        precioVenta: Double,
        unidadMedida: String,
        stock: Double
    ) = withContext(Dispatchers.IO) {
        val product = ProductEntity(
            id = id,
            nombre = nombre.trim(),
            categoria = categoria,
            costoUnitario = costoUnitario,
            precioVenta = precioVenta,
            unidadMedida = unidadMedida,
            stock = stock
        )
        if (id == 0L) {
            val newId = dao.insertProduct(product)
            dao.insertMovimiento(
                MovimientoEntity(
                    productoId = newId,
                    productoNombre = product.nombre,
                    tipo = "ajuste",
                    cantidad = stock,
                    valorUnitario = costoUnitario,
                    total = stock * costoUnitario,
                    referencia = "Stock Inicial"
                )
            )
        } else {
            dao.updateProduct(product)
        }
    }

    suspend fun deleteProduct(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteVentasByProductId(id)
        dao.deleteMermasByProductId(id)
        dao.deleteMovimientosByProductId(id)
        dao.deleteProductById(id)
    }

    suspend fun registrarVenta(
        productoId: Long,
        cantidad: Double,
        precioUnitario: Double,
        cliente: String,
        fecha: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val product = dao.getProductByIdSync(productoId)
            ?: return@withContext Result.failure(Exception("Producto no encontrado"))

        if (cantidad <= 0) {
            return@withContext Result.failure(Exception("La cantidad debe ser mayor a cero"))
        }

        if (product.stock < cantidad) {
            return@withContext Result.failure(
                Exception("Stock insuficiente. Disponible: ${product.stock} ${product.unidadMedida}")
            )
        }

        val total = cantidad * precioUnitario
        val ganancia = total - (cantidad * product.costoUnitario)

        val ventaId = dao.insertVenta(
            VentaEntity(
                productoId = productoId,
                productoNombre = product.nombre,
                cantidad = cantidad,
                precioUnitario = precioUnitario,
                total = total,
                ganancia = ganancia,
                cliente = cliente.trim(),
                fecha = fecha
            )
        )

        val nuevoStock = product.stock - cantidad
        dao.updateProductStock(productoId, nuevoStock)

        dao.insertMovimiento(
            MovimientoEntity(
                productoId = productoId,
                productoNombre = product.nombre,
                tipo = "venta",
                cantidad = -cantidad,
                valorUnitario = precioUnitario,
                total = total,
                referencia = "Venta #$ventaId",
                fecha = fecha
            )
        )

        Result.success(Unit)
    }

    suspend fun eliminarVenta(ventaId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val venta = dao.getVentaById(ventaId)
            ?: return@withContext Result.failure(Exception("Venta no encontrada"))

        val product = dao.getProductByIdSync(venta.productoId)
        if (product != null) {
            dao.updateProductStock(product.id, product.stock + venta.cantidad)
        }

        dao.deleteVentaById(ventaId)
        dao.deleteMovimientosByReferencia("Venta #$ventaId")

        Result.success(Unit)
    }

    suspend fun registrarMerma(
        productoId: Long,
        cantidad: Double,
        razon: String,
        costoPerdida: Double,
        fecha: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val product = dao.getProductByIdSync(productoId)
            ?: return@withContext Result.failure(Exception("Producto no encontrado"))

        if (cantidad <= 0) {
            return@withContext Result.failure(Exception("La cantidad debe ser mayor a cero"))
        }

        if (product.stock < cantidad) {
            return@withContext Result.failure(
                Exception("Stock insuficiente para merma. Disponible: ${product.stock} ${product.unidadMedida}")
            )
        }

        val mermaId = dao.insertMerma(
            MermaEntity(
                productoId = productoId,
                productoNombre = product.nombre,
                cantidad = cantidad,
                razon = razon,
                costoPerdida = costoPerdida,
                fecha = fecha
            )
        )

        val nuevoStock = product.stock - cantidad
        dao.updateProductStock(productoId, nuevoStock)

        dao.insertMovimiento(
            MovimientoEntity(
                productoId = productoId,
                productoNombre = product.nombre,
                tipo = "merma",
                cantidad = -cantidad,
                valorUnitario = if (cantidad > 0) costoPerdida / cantidad else 0.0,
                total = costoPerdida,
                referencia = "Merma #$mermaId ($razon)",
                fecha = fecha
            )
        )

        Result.success(Unit)
    }

    suspend fun eliminarMerma(mermaId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val merma = dao.getMermaById(mermaId)
            ?: return@withContext Result.failure(Exception("Merma no encontrada"))

        val product = dao.getProductByIdSync(merma.productoId)
        if (product != null) {
            dao.updateProductStock(product.id, product.stock + merma.cantidad)
        }

        dao.deleteMermaById(mermaId)
        dao.deleteMovimientosByReferencia("Merma #$mermaId (${merma.razon})")

        Result.success(Unit)
    }

    suspend fun ajustarStock(
        productoId: Long,
        cantidadDelta: Double,
        motivo: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val product = dao.getProductByIdSync(productoId)
            ?: return@withContext Result.failure(Exception("Producto no encontrado"))

        val nuevoStock = product.stock + cantidadDelta
        if (nuevoStock < 0) {
            return@withContext Result.failure(Exception("El stock no puede quedar negativo (Actual: ${product.stock})"))
        }

        dao.updateProductStock(productoId, nuevoStock)
        dao.insertMovimiento(
            MovimientoEntity(
                productoId = productoId,
                productoNombre = product.nombre,
                tipo = "ajuste",
                cantidad = cantidadDelta,
                valorUnitario = product.costoUnitario,
                total = kotlin.math.abs(cantidadDelta * product.costoUnitario),
                referencia = "Ajuste manual: ${motivo.ifBlank { "Corrección inventario" }}"
            )
        )

        Result.success(Unit)
    }

    suspend fun authenticate(username: String, password: String): UserEntity? = withContext(Dispatchers.IO) {
        val user = dao.getUserByUsername(username.trim())
        if (user != null && user.passwordHash == password) {
            user
        } else {
            null
        }
    }

    suspend fun createUser(username: String, password: String, role: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        if (cleanUser.length < 3) return@withContext Result.failure(Exception("El usuario debe tener al menos 3 caracteres"))
        if (password.length < 4) return@withContext Result.failure(Exception("La contraseña debe tener al menos 4 caracteres"))

        val existing = dao.getUserByUsername(cleanUser)
        if (existing != null) return@withContext Result.failure(Exception("El usuario ya existe"))

        dao.insertUser(
            UserEntity(
                username = cleanUser,
                passwordHash = password,
                role = role
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        dao.deleteUserById(userId)
        Result.success(Unit)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countUsers() == 0) {
            dao.insertUser(
                UserEntity(
                    username = "admin",
                    passwordHash = "admin123",
                    role = "administrador"
                )
            )
            dao.insertUser(
                UserEntity(
                    username = "operador",
                    passwordHash = "operador123",
                    role = "operador"
                )
            )
        }

        if (dao.countProducts() == 0) {
            val sampleProducts = listOf(
                ProductEntity(nombre = "Martillo de Uña 16oz", categoria = "Herramientas", costoUnitario = 5.50, precioVenta = 11.00, unidadMedida = "unidad", stock = 42.0),
                ProductEntity(nombre = "Destornillador Phillips 6x100", categoria = "Herramientas", costoUnitario = 2.80, precioVenta = 6.50, unidadMedida = "unidad", stock = 65.0),
                ProductEntity(nombre = "Taladro Percutor 650W", categoria = "Herramientas", costoUnitario = 42.00, precioVenta = 85.00, unidadMedida = "unidad", stock = 18.0),
                ProductEntity(nombre = "Pintura Látex Blanca 1 Galón", categoria = "Pinturas", costoUnitario = 12.00, precioVenta = 22.50, unidadMedida = "litro", stock = 28.0),
                ProductEntity(nombre = "Pintura Esmalte Azul 1L", categoria = "Pinturas", costoUnitario = 7.50, precioVenta = 15.00, unidadMedida = "litro", stock = 20.0),
                ProductEntity(nombre = "Llave Francesa 10\"", categoria = "Herramientas", costoUnitario = 9.00, precioVenta = 19.50, unidadMedida = "unidad", stock = 35.0),
                ProductEntity(nombre = "Cable Eléctrico 2x1.5mm (Rollo)", categoria = "Electricidad", costoUnitario = 0.45, precioVenta = 1.10, unidadMedida = "metro", stock = 250.0),
                ProductEntity(nombre = "Bombillo LED 12W Luz Cálida", categoria = "Electricidad", costoUnitario = 1.80, precioVenta = 4.50, unidadMedida = "unidad", stock = 80.0),
                ProductEntity(nombre = "Tubo PVC Presión 1/2\" x 3m", categoria = "Plomería", costoUnitario = 2.20, precioVenta = 4.80, unidadMedida = "metro", stock = 75.0),
                ProductEntity(nombre = "Cinta Aislante 20m Negra", categoria = "Electricidad", costoUnitario = 0.90, precioVenta = 2.50, unidadMedida = "unidad", stock = 50.0),
                ProductEntity(nombre = "Serrucho de Mano 18\"", categoria = "Herramientas", costoUnitario = 14.00, precioVenta = 28.00, unidadMedida = "unidad", stock = 14.0),
                ProductEntity(nombre = "Clavos de Acero 2\" (Caja 1kg)", categoria = "Ferretería", costoUnitario = 1.20, precioVenta = 2.80, unidadMedida = "caja", stock = 120.0),
                ProductEntity(nombre = "Tornillo Drywall 1-1/4 (Caja 100u)", categoria = "Ferretería", costoUnitario = 2.10, precioVenta = 4.90, unidadMedida = "caja", stock = 90.0),
                ProductEntity(nombre = "Llave de Paso 1/2\" Bronce", categoria = "Plomería", costoUnitario = 5.00, precioVenta = 11.50, unidadMedida = "unidad", stock = 30.0)
            )

            val createdProductIds = mutableListOf<Long>()
            for (p in sampleProducts) {
                val pId = dao.insertProduct(p)
                createdProductIds.add(pId)
                dao.insertMovimiento(
                    MovimientoEntity(
                        productoId = pId,
                        productoNombre = p.nombre,
                        tipo = "ajuste",
                        cantidad = p.stock,
                        valorUnitario = p.costoUnitario,
                        total = p.stock * p.costoUnitario,
                        referencia = "Inventario Inicial"
                    )
                )
            }

            // Seed historical sales for the last 20 days
            val now = System.currentTimeMillis()
            val dayMillis = 86_400_000L
            val clients = listOf("Constructora Sol", "Juan Pérez", "Taller Martínez", "María Gómez", "Electricidad Rápida", "Carlos Ruiz", "Ferretería Vecina", "Ana Torres")
            val reasons = listOf("Daño", "Devolución", "Robo", "Caducidad")

            for (dayOffset in 0..18) {
                val date = now - (dayOffset * dayMillis)
                val salesCount = Random.nextInt(2, 5)
                for (s in 0 until salesCount) {
                    val pIndex = Random.nextInt(sampleProducts.size)
                    val p = sampleProducts[pIndex]
                    val pId = createdProductIds[pIndex]
                    val qty = Random.nextInt(1, 4).toDouble()
                    val total = qty * p.precioVenta
                    val ganancia = total - (qty * p.costoUnitario)
                    val client = clients[Random.nextInt(clients.size)]

                    val vId = dao.insertVenta(
                        VentaEntity(
                            productoId = pId,
                            productoNombre = p.nombre,
                            cantidad = qty,
                            precioUnitario = p.precioVenta,
                            total = total,
                            ganancia = ganancia,
                            cliente = client,
                            fecha = date
                        )
                    )
                    dao.insertMovimiento(
                        MovimientoEntity(
                            productoId = pId,
                            productoNombre = p.nombre,
                            tipo = "venta",
                            cantidad = -qty,
                            valorUnitario = p.precioVenta,
                            total = total,
                            referencia = "Venta #$vId",
                            fecha = date
                        )
                    )
                }

                // Occasionally seed a merma
                if (dayOffset % 4 == 0) {
                    val pIndex = Random.nextInt(sampleProducts.size)
                    val p = sampleProducts[pIndex]
                    val pId = createdProductIds[pIndex]
                    val qty = 1.0
                    val cost = qty * p.costoUnitario
                    val reason = reasons[Random.nextInt(reasons.size)]

                    val mId = dao.insertMerma(
                        MermaEntity(
                            productoId = pId,
                            productoNombre = p.nombre,
                            cantidad = qty,
                            razon = reason,
                            costoPerdida = cost,
                            fecha = date
                        )
                    )
                    dao.insertMovimiento(
                        MovimientoEntity(
                            productoId = pId,
                            productoNombre = p.nombre,
                            tipo = "merma",
                            cantidad = -qty,
                            valorUnitario = p.costoUnitario,
                            total = cost,
                            referencia = "Merma #$mId ($reason)",
                            fecha = date
                        )
                    )
                }
            }
        }
    }
}
