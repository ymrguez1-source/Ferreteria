package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FerreteriaDatabase
import com.example.data.FerreteriaRepository
import com.example.data.MermaEntity
import com.example.data.MovimientoEntity
import com.example.data.ProductEntity
import com.example.data.UserEntity
import com.example.data.VentaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class FerreteriaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FerreteriaRepository
    private val prefs = application.getSharedPreferences("ferreteria_prefs", Context.MODE_PRIVATE)

    val products: StateFlow<List<ProductEntity>>
    val ventas: StateFlow<List<VentaEntity>>
    val mermas: StateFlow<List<MermaEntity>>
    val movimientos: StateFlow<List<MovimientoEntity>>
    val users: StateFlow<List<UserEntity>>

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isLicenseActive = MutableStateFlow(true)
    val isLicenseActive: StateFlow<Boolean> = _isLicenseActive.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Filters & Search
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _productCategoryFilter = MutableStateFlow("Todas")
    val productCategoryFilter: StateFlow<String> = _productCategoryFilter.asStateFlow()

    init {
        val database = FerreteriaDatabase.getDatabase(application)
        repository = FerreteriaRepository(database.ferreteriaDao())

        products = repository.allProducts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        ventas = repository.allVentas.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        mermas = repository.allMermas.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        movimientos = repository.allMovimientos.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        users = repository.allUsers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Init Device ID
        var savedDevId = prefs.getString("device_id", null)
        if (savedDevId.isNullOrEmpty()) {
            savedDevId = "FERR-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
            prefs.edit().putString("device_id", savedDevId).apply()
        }
        _deviceId.value = savedDevId

        // License check (default active for instant usability, customizable in Settings)
        val isLic = prefs.getBoolean("license_active", true)
        _isLicenseActive.value = isLic

        // Auto login default admin if not set
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            val adminUser = repository.authenticate("admin", "admin123")
            if (_currentUser.value == null && adminUser != null) {
                _currentUser.value = adminUser
            }
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun setProductSearch(query: String) {
        _productSearchQuery.value = query
    }

    fun setProductCategory(category: String) {
        _productCategoryFilter.value = category
    }

    fun saveProduct(
        id: Long = 0,
        nombre: String,
        categoria: String,
        costoUnitario: Double,
        precioVenta: Double,
        unidadMedida: String,
        stock: Double
    ) {
        viewModelScope.launch {
            try {
                repository.saveProduct(
                    id = id,
                    nombre = nombre,
                    categoria = categoria,
                    costoUnitario = costoUnitario,
                    precioVenta = precioVenta,
                    unidadMedida = unidadMedida,
                    stock = stock
                )
                showSnackbar(if (id == 0L) "Producto \"$nombre\" agregado" else "Producto actualizado")
            } catch (e: Exception) {
                showSnackbar("Error al guardar producto: ${e.message}")
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(id)
                showSnackbar("Producto eliminado correctamente")
            } catch (e: Exception) {
                showSnackbar("Error al eliminar producto: ${e.message}")
            }
        }
    }

    fun registrarVenta(
        productoId: Long,
        cantidad: Double,
        precioUnitario: Double,
        cliente: String,
        fecha: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val result = repository.registrarVenta(productoId, cantidad, precioUnitario, cliente, fecha)
            result.onSuccess {
                showSnackbar("¡Venta registrada con éxito!")
            }.onFailure {
                showSnackbar(it.message ?: "Error al registrar venta")
            }
        }
    }

    fun eliminarVenta(ventaId: Long) {
        viewModelScope.launch {
            val result = repository.eliminarVenta(ventaId)
            result.onSuccess {
                showSnackbar("Venta cancelada y stock restaurado")
            }.onFailure {
                showSnackbar(it.message ?: "Error al eliminar venta")
            }
        }
    }

    fun registrarMerma(
        productoId: Long,
        cantidad: Double,
        razon: String,
        costoPerdida: Double,
        fecha: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val result = repository.registrarMerma(productoId, cantidad, razon, costoPerdida, fecha)
            result.onSuccess {
                showSnackbar("Merma registrada y stock actualizado")
            }.onFailure {
                showSnackbar(it.message ?: "Error al registrar merma")
            }
        }
    }

    fun eliminarMerma(mermaId: Long) {
        viewModelScope.launch {
            val result = repository.eliminarMerma(mermaId)
            result.onSuccess {
                showSnackbar("Merma eliminada y stock restaurado")
            }.onFailure {
                showSnackbar(it.message ?: "Error al eliminar merma")
            }
        }
    }

    fun ajustarStock(productoId: Long, cantidadDelta: Double, motivo: String) {
        viewModelScope.launch {
            val result = repository.ajustarStock(productoId, cantidadDelta, motivo)
            result.onSuccess {
                showSnackbar("Stock ajustado correctamente")
            }.onFailure {
                showSnackbar(it.message ?: "Error al ajustar stock")
            }
        }
    }

    fun login(username: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.authenticate(username, pass)
            if (user != null) {
                _currentUser.value = user
                showSnackbar("Bienvenido, ${user.username}")
                onSuccess()
            } else {
                showSnackbar("Usuario o contraseña incorrectos")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        showSnackbar("Sesión cerrada")
    }

    fun createUser(username: String, pass: String, role: String) {
        viewModelScope.launch {
            val res = repository.createUser(username, pass, role)
            res.onSuccess {
                showSnackbar("Usuario \"$username\" creado como $role")
            }.onFailure {
                showSnackbar(it.message ?: "Error al crear usuario")
            }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            val res = repository.deleteUser(userId)
            res.onSuccess {
                showSnackbar("Usuario eliminado")
            }.onFailure {
                showSnackbar(it.message ?: "Error al eliminar usuario")
            }
        }
    }

    fun activateLicense(key: String): Boolean {
        val trimmed = key.trim().uppercase()
        if (trimmed == "FERR-2026-PRO" || trimmed == "ACTIVA-2026" || trimmed.startsWith("FERR-") || trimmed.length >= 6) {
            prefs.edit().putBoolean("license_active", true).putString("license_key", trimmed).apply()
            _isLicenseActive.value = true
            showSnackbar("¡Licencia activada con éxito!")
            return true
        } else {
            showSnackbar("Clave inválida. Introduce una clave válida.")
            return false
        }
    }

    fun deactivateLicense() {
        prefs.edit().putBoolean("license_active", false).apply()
        _isLicenseActive.value = false
        showSnackbar("Sistema bloqueado. Se requiere activación.")
    }

    fun generateProductsExcelCsv(): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // BOM for Excel UTF-8
        sb.append("Código;Nombre;Categoría;Unidad;Stock;Costo Unitario ($);Precio Venta ($);Margen (%)\r\n")
        products.value.forEach { p ->
            val margin = if (p.precioVenta > 0) (((p.precioVenta - p.costoUnitario) / p.precioVenta) * 100).toInt() else 100
            sb.append("${p.id};\"${p.nombre}\";\"${p.categoria}\";\"${p.unidadMedida}\";${p.stock};${p.costoUnitario};${p.precioVenta};$margin%\r\n")
        }
        return sb.toString()
    }

    fun importProductsFromCsv(csvText: String): Pair<Int, Int> {
        var created = 0
        var updated = 0
        val lines = csvText.lines()
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed

            val sep = if (line.contains(';')) ';' else ','
            val cols = line.split(sep).map { it.trim().removeSurrounding("\"") }
            if (cols.size < 4) return@forEachIndexed

            val name = cols[0]
            if (index == 0 && (name.equals("nombre", ignoreCase = true) || name.equals("producto", ignoreCase = true) || name.equals("código", ignoreCase = true))) {
                return@forEachIndexed
            }

            val category = cols.getOrNull(1)?.ifEmpty { "Ferretería" } ?: "Ferretería"
            val unit = cols.getOrNull(2)?.ifEmpty { "Unidades" } ?: "Unidades"
            val stock = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
            val cost = cols.getOrNull(4)?.toDoubleOrNull() ?: 0.0
            val price = cols.getOrNull(5)?.toDoubleOrNull() ?: (cost * 1.5)

            val existing = products.value.find { it.nombre.equals(name, ignoreCase = true) }
            if (existing != null) {
                saveProduct(existing.id, name, category, cost, price, unit, stock)
                updated++
            } else {
                saveProduct(0L, name, category, cost, price, unit, stock)
                created++
            }
        }
        showSnackbar("Importación Excel: $created nuevos, $updated actualizados")
        return Pair(created, updated)
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            showSnackbar("Datos de prueba recargados")
        }
    }
}
