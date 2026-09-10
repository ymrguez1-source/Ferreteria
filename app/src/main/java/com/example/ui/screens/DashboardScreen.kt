package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MermaEntity
import com.example.data.ProductEntity
import com.example.data.VentaEntity
import com.example.ui.components.BarChartItem
import com.example.ui.components.DonutSlice
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SimpleDonutChart
import com.example.ui.components.StatCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ForestSuccess
import com.example.ui.theme.SteelSecondary
import com.example.ui.theme.TerracottaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    products: List<ProductEntity>,
    ventas: List<VentaEntity>,
    mermas: List<MermaEntity>,
    onNavigateToVentas: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onOpenAddVenta: () -> Unit,
    onOpenAddProducto: () -> Unit,
    onOpenAddMerma: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val thirtyDaysAgo = now - (30L * 86_400_000L)
    val todayStart = remember(now) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date(now))
        sdf.parse(todayStr)?.time ?: (now - 86_400_000L)
    }

    // 30 day metrics
    val ventas30d = remember(ventas) { ventas.filter { it.fecha >= thirtyDaysAgo } }
    val mermas30d = remember(mermas) { mermas.filter { it.fecha >= thirtyDaysAgo } }

    val totalGanancias30d = remember(ventas30d) { ventas30d.sumOf { it.ganancia } }
    val totalVentas30d = remember(ventas30d) { ventas30d.sumOf { it.total } }
    val totalMermas30d = remember(mermas30d) { mermas30d.sumOf { it.costoPerdida } }

    // Top selling product in last 30d
    val topProduct = remember(ventas30d) {
        ventas30d.groupBy { it.productoNombre }
            .mapValues { entry -> entry.value.sumOf { it.cantidad } }
            .maxByOrNull { it.value }
    }

    // Today's sales breakdown
    val ventasHoy = remember(ventas) { ventas.filter { it.fecha >= todayStart } }
    val hoyTotal = remember(ventasHoy) { ventasHoy.sumOf { it.total } }
    val hoyGanancia = remember(ventasHoy) { ventasHoy.sumOf { it.ganancia } }

    // Daily sales for bar chart (last 7 days)
    val chartItems = remember(ventas) {
        val dayFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))
        (6 downTo 0).map { dayOffset ->
            val dayTime = now - (dayOffset * 86_400_000L)
            val dayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dayTime))
            val dayTotal = ventas.filter {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.fecha)) == dayDateStr
            }.sumOf { it.total }
            BarChartItem(
                label = dayFormat.format(Date(dayTime)),
                value = dayTotal,
                formattedValue = "$${String.format("%.0f", dayTotal)}"
            )
        }
    }

    // Mermas by reason
    val donutSlices = remember(mermas30d) {
        val colorMap = mapOf(
            "Robo" to CrimsonDanger,
            "Daño" to TerracottaPrimary,
            "Caducidad" to AmberAccent,
            "Devolución" to SteelSecondary,
            "Otro" to Color(0xFF78909C)
        )
        mermas30d.groupBy { it.razon }
            .map { (razon, list) ->
                DonutSlice(
                    label = razon,
                    value = list.sumOf { it.costoPerdida },
                    color = colorMap[razon] ?: Color(0xFF9E9E9E)
                )
            }
            .sortedByDescending { it.value }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Quick Action Row
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Panel de Control",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(Date()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = TerracottaPrimary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAddVenta,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Venta", maxLines = 1, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onOpenAddProducto,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SteelSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Producto", maxLines = 1, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenAddMerma,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrimsonDanger)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merma", maxLines = 1, fontSize = 13.sp, color = CrimsonDanger)
                        }
                    }
                }
            }
        }

        // Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Ganancias (30d)",
                        value = "$${String.format("%.2f", totalGanancias30d)}",
                        subtitle = "Margen positivo",
                        accentColor = ForestSuccess,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Ventas (30d)",
                        value = "$${String.format("%.2f", totalVentas30d)}",
                        subtitle = "${ventas30d.size} operaciones",
                        accentColor = SteelSecondary,
                        icon = Icons.Default.PointOfSale,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Mermas (30d)",
                        value = "$${String.format("%.2f", totalMermas30d)}",
                        subtitle = "${mermas30d.size} registros",
                        accentColor = CrimsonDanger,
                        icon = Icons.Default.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Más Vendido",
                        value = topProduct?.key?.take(13) ?: "N/A",
                        subtitle = topProduct?.let { "${it.value.toInt()} unidades" } ?: "Sin datos",
                        accentColor = AmberAccent,
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Today's Breakdown Card (Desglose del Día)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Desglose de Ventas de Hoy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ForestSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Hoy: $${String.format("%.2f", hoyTotal)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ForestSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (ventasHoy.isEmpty()) {
                        Text(
                            text = "Aún no se han registrado ventas hoy. Presione \"Venta\" para iniciar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ventasHoy.take(4).forEach { v ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = v.productoNombre,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "${v.cantidad} unidades ${if (v.cliente.isNotBlank()) "• ${v.cliente}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${String.format("%.2f", v.total)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "+$${String.format("%.2f", v.ganancia)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = ForestSuccess
                                        )
                                    }
                                }
                            }
                            if (ventasHoy.size > 4) {
                                TextButton(
                                    onClick = onNavigateToVentas,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Ver todas las ${ventasHoy.size} ventas de hoy")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Sales Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ventas de los Últimos 7 Días",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SimpleBarChart(items = chartItems, barColor = TerracottaPrimary)
                }
            }
        }

        // Mermas Breakdown
        if (donutSlices.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Distribución de Mermas y Pérdidas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SimpleDonutChart(slices = donutSlices)
                    }
                }
            }
        }
    }
}
