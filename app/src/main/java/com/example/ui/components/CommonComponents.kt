package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ForestSuccess
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.WarmWarning

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = TerracottaPrimary,
    icon: ImageVector? = null
) {
    Card(
        modifier = modifier.testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(accentColor, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.12f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(categoria: String) {
    val (bg, textColor) = when (categoria.lowercase()) {
        "herramientas" -> Color(0xFFE3F2FD) to Color(0xFF0D47A1)
        "pinturas" -> Color(0xFFEDE7F6) to Color(0xFF4A148C)
        "electricidad" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        "plomería", "plomeria" -> Color(0xFFE0F7FA) to Color(0xFF006064)
        "ferretería", "ferreteria" -> Color(0xFFFDF0EB) to TerracottaPrimary
        else -> Color(0xFFEEEEEE) to Color(0xFF424242)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            text = categoria,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StockBadge(stock: Double, unidad: String) {
    val isLow = stock <= 5.0
    val bg = if (isLow) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
    val fg = if (isLow) CrimsonDanger else ForestSuccess

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "Stock: $stock $unidad",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// --- DIALOG: ADD/EDIT PRODUCT ---
@Composable
fun ProductDialog(
    initialProduct: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (nombre: String, categoria: String, costo: Double, precio: Double, unidad: String, stock: Double) -> Unit
) {
    var nombre by remember { mutableStateOf(initialProduct?.nombre ?: "") }
    var categoria by remember { mutableStateOf(initialProduct?.categoria ?: "Herramientas") }
    var costoStr by remember { mutableStateOf(initialProduct?.costoUnitario?.toString() ?: "") }
    var precioStr by remember { mutableStateOf(initialProduct?.precioVenta?.toString() ?: "") }
    var unidad by remember { mutableStateOf(initialProduct?.unidadMedida ?: "unidad") }
    var stockStr by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }

    var catMenuOpen by remember { mutableStateOf(false) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Herramientas", "Pinturas", "Ferretería", "Electricidad", "Plomería", "Otros")
    val units = listOf("unidad", "kg", "litro", "metro", "caja")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "Nuevo Producto" else "Editar Producto",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Producto") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = {
                            IconButton(onClick = { catMenuOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { catMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = catMenuOpen,
                        onDismissRequest = { catMenuOpen = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categoria = cat
                                    catMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costoStr,
                        onValueChange = { costoStr = it },
                        label = { Text("Costo ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = precioStr,
                        onValueChange = { precioStr = it },
                        label = { Text("Precio ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Unit Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = unidad,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = {
                                IconButton(onClick = { unitMenuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Unidad")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { unitMenuOpen = true }
                        )
                        DropdownMenu(
                            expanded = unitMenuOpen,
                            onDismissRequest = { unitMenuOpen = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unidad = u
                                        unitMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val costo = costoStr.toDoubleOrNull()
                    val precio = precioStr.toDoubleOrNull()
                    val stock = stockStr.toDoubleOrNull()

                    if (nombre.isBlank()) {
                        errorText = "El nombre no puede estar vacío"
                        return@Button
                    }
                    if (costo == null || costo < 0) {
                        errorText = "Costo unitario inválido"
                        return@Button
                    }
                    if (precio == null || precio < 0) {
                        errorText = "Precio de venta inválido"
                        return@Button
                    }
                    if (stock == null || stock < 0) {
                        errorText = "Stock inválido"
                        return@Button
                    }

                    onSave(nombre, categoria, costo, precio, unidad, stock)
                },
                modifier = Modifier.testTag("btn_save_product")
            ) {
                Text(if (initialProduct == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- DIALOG: ADD VENTA ---
@Composable
fun AddVentaDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (productoId: Long, cantidad: Double, precio: Double, cliente: String) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    val selectedProduct = products.firstOrNull { it.id == selectedProductId }

    var cantidadStr by remember { mutableStateOf("1") }
    var precioStr by remember { mutableStateOf(selectedProduct?.precioVenta?.toString() ?: "0.0") }
    var cliente by remember { mutableStateOf("") }
    var prodMenuOpen by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val cantidad = cantidadStr.toDoubleOrNull() ?: 0.0
    val precio = precioStr.toDoubleOrNull() ?: 0.0
    val total = cantidad * precio
    val ganancia = if (selectedProduct != null) total - (cantidad * selectedProduct.costoUnitario) else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Registrar Venta", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Product selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedProduct?.let { "${it.nombre} (Stock: ${it.stock})" } ?: "Seleccione Producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        trailingIcon = {
                            IconButton(onClick = { prodMenuOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Productos")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { prodMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = prodMenuOpen,
                        onDismissRequest = { prodMenuOpen = false }
                    ) {
                        products.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.nombre} (Disp: ${p.stock} ${p.unidadMedida})") },
                                onClick = {
                                    selectedProductId = p.id
                                    precioStr = p.precioVenta.toString()
                                    prodMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidadStr,
                        onValueChange = { cantidadStr = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = precioStr,
                        onValueChange = { precioStr = it },
                        label = { Text("Precio Unit. ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = cliente,
                    onValueChange = { cliente = it },
                    label = { Text("Cliente (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live calculations box
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total a Cobrar:", style = MaterialTheme.typography.bodyMedium)
                            Text("$${String.format("%.2f", total)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ganancia Estimada:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$${String.format("%.2f", ganancia)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (ganancia >= 0) ForestSuccess else CrimsonDanger
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProduct == null) {
                        errorText = "Seleccione un producto"
                        return@Button
                    }
                    if (cantidad <= 0) {
                        errorText = "La cantidad debe ser mayor a 0"
                        return@Button
                    }
                    if (cantidad > selectedProduct.stock) {
                        errorText = "Stock insuficiente (disponible: ${selectedProduct.stock})"
                        return@Button
                    }
                    if (precio <= 0) {
                        errorText = "El precio debe ser mayor a 0"
                        return@Button
                    }

                    onConfirm(selectedProductId, cantidad, precio, cliente)
                },
                modifier = Modifier.testTag("btn_confirm_sale")
            ) {
                Text("Registrar Venta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- DIALOG: ADD MERMA ---
@Composable
fun AddMermaDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (productoId: Long, cantidad: Double, razon: String, costoPerdida: Double) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    val selectedProduct = products.firstOrNull { it.id == selectedProductId }

    var cantidadStr by remember { mutableStateOf("1") }
    var razon by remember { mutableStateOf("Daño") }
    var prodMenuOpen by remember { mutableStateOf(false) }
    var razonMenuOpen by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val razones = listOf("Robo", "Daño", "Caducidad", "Devolución", "Otro")
    val cantidad = cantidadStr.toDoubleOrNull() ?: 0.0
    val costoPerdida = if (selectedProduct != null) cantidad * selectedProduct.costoUnitario else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Registrar Merma / Pérdida", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Product selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedProduct?.let { "${it.nombre} (Stock: ${it.stock})" } ?: "Seleccionar Producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto Afectado") },
                        trailingIcon = {
                            IconButton(onClick = { prodMenuOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Productos")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { prodMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = prodMenuOpen,
                        onDismissRequest = { prodMenuOpen = false }
                    ) {
                        products.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.nombre} (Stock: ${p.stock})") },
                                onClick = {
                                    selectedProductId = p.id
                                    prodMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidadStr,
                        onValueChange = { cantidadStr = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Razon
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = razon,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Causa") },
                            trailingIcon = {
                                IconButton(onClick = { razonMenuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Causas")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { razonMenuOpen = true }
                        )
                        DropdownMenu(
                            expanded = razonMenuOpen,
                            onDismissRequest = { razonMenuOpen = false }
                        ) {
                            razones.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        razon = r
                                        razonMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pérdida Monetaria:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$${String.format("%.2f", costoPerdida)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonDanger
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProduct == null) {
                        errorText = "Seleccione un producto"
                        return@Button
                    }
                    if (cantidad <= 0) {
                        errorText = "Cantidad debe ser mayor a cero"
                        return@Button
                    }
                    if (cantidad > selectedProduct.stock) {
                        errorText = "Cantidad excede el stock disponible (${selectedProduct.stock})"
                        return@Button
                    }

                    onConfirm(selectedProductId, cantidad, razon, costoPerdida)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
            ) {
                Text("Registrar Merma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- DIALOG: STOCK ADJUSTMENT ---
@Composable
fun AdjustStockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (cantidadDelta: Double, motivo: String) -> Unit
) {
    var deltaStr by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("Conteo físico de inventario") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val delta = deltaStr.toDoubleOrNull() ?: 0.0
    val newStock = product.stock + delta

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajustar Stock", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${product.nombre} (Actual: ${product.stock} ${product.unidadMedida})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = deltaStr,
                    onValueChange = { deltaStr = it },
                    label = { Text("Variación (Ej: +10 ó -5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo del Ajuste") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nuevo Stock Resultante:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$newStock ${product.unidadMedida}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (delta == 0.0) {
                        errorText = "Ingrese una cantidad distinta de cero"
                        return@Button
                    }
                    if (newStock < 0) {
                        errorText = "El nuevo stock no puede ser negativo"
                        return@Button
                    }
                    onConfirm(delta, motivo)
                }
            ) {
                Text("Aplicar Ajuste")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
