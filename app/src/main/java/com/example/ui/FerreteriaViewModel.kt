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

    private val _isLicenseActive = MutableStateFlow(false)
    val isLicenseActive: StateFlow<Boolean> = _isLicenseActive.asStateFlow()

    private val _licenseInfo = MutableStateFlow<String?>(null)
    val licenseInfo: StateFlow<String?> = _licenseInfo.asStateFlow()

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

        // Init Device ID (clean uppercase format compatible with generator)
        var savedDevId = prefs.getString("device_id", null)
        if (savedDevId.isNullOrEmpty()) {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            val randomStr = (1..12).map { chars.random() }.joinToString("")
            savedDevId = "FERR-$randomStr"
            prefs.edit().putString("device_id", savedDevId).apply()
        }
        _deviceId.value = savedDevId

        // License check with annexed validation algorithm (NO MASTER KEYS)
        val storedKey = prefs.getString("license_key", null)
        if (!storedKey.isNullOrBlank()) {
            val res = validateAnnexedLicense(savedDevId, storedKey)
            if (res.first && res.second is LicenseData) {
                val data = res.second as LicenseData
                _isLicenseActive.value = true
                _licenseInfo.value = "${data.client} (${data.type.uppercase()}) - Vence: ${data.expiry}"
            } else {
                _isLicenseActive.value = false
                _licenseInfo.value = null
                prefs.edit().putBoolean("license_active", false).apply()
            }
        } else {
            _isLicenseActive.value = false
            _licenseInfo.value = null
        }

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

    data class LicenseData(
        val deviceId: String,
        val client: String,
        val type: String,
        val start: String,
        val expiry: String,
        val duration: Int,
        val id: String
    )

    fun validateAnnexedLicense(currentDeviceId: String, rawKey: String): Pair<Boolean, Any?> {
        try {
            val cleanKey = rawKey.trim()
            if (cleanKey.length < 15) {
                return Pair(false, "Clave de licencia inválida (longitud insuficiente)")
            }

            val receivedSig = cleanKey.substring(4, 14)
            val encoded = cleanKey.substring(14)
            val jsonBytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            val json = org.json.JSONObject(jsonStr)

            val devId = json.optString("deviceId")
            val client = json.optString("client")
            val type = json.optString("type")
            val start = json.optString("start")
            val expiry = json.optString("expiry")
            val duration = json.optInt("duration", 0)
            val id = json.optString("id")

            val signString = "$devId|$client|$type|$start|$expiry|$duration|$id"

            // Compute JS 32-bit integer hash: ((hash shl 5) - hash) + charCode
            var hash = 0
            for (ch in signString) {
                val code = ch.code
                hash = ((hash shl 5) - hash) + code
            }

            val absHash: Long = if (hash < 0) {
                if (hash == Int.MIN_VALUE) 2147483648L else (-hash).toLong()
            } else {
                hash.toLong()
            }
            val expectedSig = java.lang.Long.toString(absHash, 36).uppercase().padStart(10, '0')

            if (expectedSig != receivedSig) {
                return Pair(false, "Firma digital de licencia inválida")
            }

            if (devId != currentDeviceId) {
                return Pair(false, "El ID de dispositivo ($devId) no coincide con este terminal ($currentDeviceId)")
            }

            // Expiry check
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
            val expiryDate = sdf.parse(expiry.substring(0, 10))
            val startDate = sdf.parse(start.substring(0, 10))
            val todayCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            if (expiryDate != null && todayCal.after(expiryDate)) {
                return Pair(false, "La licencia ha expirado el $expiry")
            }

            if (startDate != null && todayCal.before(startDate)) {
                return Pair(false, "La licencia no está activa aún (inicia el $start)")
            }

            val data = LicenseData(devId, client, type, start, expiry, duration, id)
            return Pair(true, data)
        } catch (e: Exception) {
            return Pair(false, "Clave corrupta o formato inválido")
        }
    }

    fun activateLicense(key: String): Boolean {
        val trimmed = key.trim()
        val result = validateAnnexedLicense(_deviceId.value, trimmed)
        if (result.first && result.second is LicenseData) {
            val data = result.second as LicenseData
            prefs.edit()
                .putBoolean("license_active", true)
                .putString("license_key", trimmed)
                .putString("license_client", data.client)
                .putString("license_type", data.type)
                .putString("license_expiry", data.expiry)
                .putString("license_start", data.start)
                .apply()
            _isLicenseActive.value = true
            _licenseInfo.value = "${data.client} (${data.type.uppercase()}) - Vence: ${data.expiry}"
            showSnackbar("¡Licencia activada para ${data.client} (${data.type.uppercase()})!")
            return true
        } else {
            val errorMsg = result.second as? String ?: "Clave inválida. Introduce la clave generada para este terminal."
            showSnackbar(errorMsg)
            return false
        }
    }

    fun deactivateLicense() {
        prefs.edit()
            .putBoolean("license_active", false)
            .remove("license_key")
            .apply()
        _isLicenseActive.value = false
        _licenseInfo.value = null
        showSnackbar("Sistema bloqueado. Se requiere activación autorizada.")
    }

    fun generateProductsExcelWorkbook(): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        sb.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")
        sb.append(" <Styles>\n")
        sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
        sb.append("   <Alignment ss:Vertical=\"Center\"/>\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#1A1A1A\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"HeaderStyle\">\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
        sb.append("   <Interior ss:Color=\"#107C41\" ss:Pattern=\"Solid\"/>\n")
        sb.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
        sb.append("   <Borders>\n")
        sb.append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0B5F30\"/>\n")
        sb.append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0B5F30\"/>\n")
        sb.append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0B5F30\"/>\n")
        sb.append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0B5F30\"/>\n")
        sb.append("   </Borders>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"DataCell\">\n")
        sb.append("   <Borders>\n")
        sb.append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("   </Borders>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"CurrencyCell\">\n")
        sb.append("   <Alignment ss:Horizontal=\"Right\"/>\n")
        sb.append("   <NumberFormat ss:Format=\"$#,##0.00\"/>\n")
        sb.append("   <Borders>\n")
        sb.append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#DCDCDC\"/>\n")
        sb.append("   </Borders>\n")
        sb.append("  </Style>\n")
        sb.append(" </Styles>\n")
        sb.append(" <Worksheet ss:Name=\"Inventario\">\n")
        sb.append("  <Table ss:DefaultRowHeight=\"20\">\n")

        val headers = listOf("Código", "Nombre", "Categoría", "Unidad", "Stock", "Costo ($)", "Precio ($)", "Margen (%)")
        sb.append("   <Row ss:Height=\"24\">\n")
        headers.forEach { h ->
            sb.append("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">$h</Data></Cell>\n")
        }
        sb.append("   </Row>\n")

        products.value.forEach { p ->
            val margin = if (p.precioVenta > 0) (((p.precioVenta - p.costoUnitario) / p.precioVenta) * 100).toInt() else 100
            val escName = p.nombre.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
            val escCat = p.categoria.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
            val escUnit = p.unidadMedida.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

            sb.append("   <Row>\n")
            sb.append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"Number\">${p.id}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$escName</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$escCat</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$escUnit</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"Number\">${p.stock}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"CurrencyCell\"><Data ss:Type=\"Number\">${p.costoUnitario}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"CurrencyCell\"><Data ss:Type=\"Number\">${p.precioVenta}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$margin%</Data></Cell>\n")
            sb.append("   </Row>\n")
        }

        sb.append("  </Table>\n")
        sb.append(" </Worksheet>\n")
        sb.append("</Workbook>")
        return sb.toString()
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
