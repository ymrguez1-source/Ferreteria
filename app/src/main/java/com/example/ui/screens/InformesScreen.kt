package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MermaEntity
import com.example.data.MovimientoEntity
import com.example.data.ProductEntity
import com.example.data.VentaEntity
import com.example.ui.components.CategoryBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ForestSuccess
import com.example.ui.theme.SteelSecondary
import com.example.ui.theme.TerracottaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InformesScreen(
    products: List<ProductEntity>,
    ventas: List<VentaEntity>,
    mermas: List<MermaEntity>,
    movimientos: List<MovimientoEntity>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPeriod by remember { mutableStateOf("30 Días") }
    val periods = listOf("Hoy", "7 Días", "30 Días", "Año", "Todo")

    val now = System.currentTimeMillis()
    val periodStart = remember(selectedPeriod, now) {
        when (selectedPeriod) {
            "Hoy" -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(sdf.format(Date(now)))?.time ?: (now - 86_400_000L)
            }
            "7 Días" -> now - (7L * 86_400_000L)
            "30 Días" -> now - (30L * 86_400_000L)
            "Año" -> now - (365L * 86_400_000L)
            else -> 0L
        }
    }

    val filteredVentas = remember(ventas, periodStart) {
        ventas.filter { it.fecha >= periodStart }
    }
    val filteredMermas = remember(mermas, periodStart) {
        mermas.filter { it.fecha >= periodStart }
    }

    val totalVentas = remember(filteredVentas) { filteredVentas.sumOf { it.total } }
    val totalGanancias = remember(filteredVentas) { filteredVentas.sumOf { it.ganancia } }
    val totalPerdidas = remember(filteredMermas) { filteredMermas.sumOf { it.costoPerdida } }
    val totalUnidades = remember(filteredVentas) { filteredVentas.sumOf { it.cantidad } }
    val gananciaNeta = totalGanancias - totalPerdidas

    // Most profitable products ranking
    val rankingRentables = remember(filteredVentas, products) {
        filteredVentas.groupBy { it.productoId }
            .map { (pid, sales) ->
                val prod = products.firstOrNull { it.id == pid }
                val units = sales.sumOf { it.cantidad }
                val totalRec = sales.sumOf { it.total }
                val profit = sales.sumOf { it.ganancia }
                val margin = if (totalRec > 0) (profit / totalRec * 100).toInt() else 0
                RankedProduct(
                    nombre = prod?.nombre ?: sales.first().productoNombre,
                    categoria = prod?.categoria ?: "General",
                    unidades = units,
                    ganancia = profit,
                    margen = margin
                )
            }
            .sortedByDescending { it.ganancia }
            .take(8)
    }

    // Causes of loss
    val causesOfLoss = remember(filteredMermas) {
        val total = filteredMermas.sumOf { it.costoPerdida }
        filteredMermas.groupBy { it.razon }
            .map { (razon, list) ->
                val sum = list.sumOf { it.costoPerdida }
                val pct = if (total > 0) (sum / total * 100).toInt() else 0
                LossCause(razon = razon, count = list.size, totalCost = sum, percentage = pct)
            }
            .sortedByDescending { it.totalCost }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Informes & Métricas", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Historial Auditoría", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTab == 0) {
            // INFORMES CONTENT
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Period filter pills
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(periods) { p ->
                            val isSelected = p == selectedPeriod
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedPeriod = p }
                            ) {
                                Text(
                                    text = p,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Stats 2x2
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Ganancia Neta",
                                value = "$${String.format("%.2f", gananciaNeta)}",
                                subtitle = "Descontando mermas",
                                accentColor = ForestSuccess,
                                icon = Icons.Default.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Total Facturado",
                                value = "$${String.format("%.2f", totalVentas)}",
                                subtitle = "$totalUnidades unidades vendidas",
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
                                title = "Mermas Totales",
                                value = "-$${String.format("%.2f", totalPerdidas)}",
                                subtitle = "${filteredMermas.size} pérdidas",
                                accentColor = CrimsonDanger,
                                icon = Icons.Default.TrendingDown,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Margen Bruto",
                                value = if (totalVentas > 0) "${(totalGanancias / totalVentas * 100).toInt()}%" else "0%",
                                subtitle = "Retorno s/ costo",
                                accentColor = Color(0xFFFFA000),
                                icon = Icons.Default.Assessment,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Top Profitable Products
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Productos Más Rentables",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (rankingRentables.isEmpty()) {
                                Text(
                                    text = "Sin ventas en este período",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rankingRentables.forEachIndexed { idx, p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = TerracottaPrimary.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = TerracottaPrimary
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = p.nombre,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                    Text(
                                                        text = "${p.categoria} • ${p.unidades} uds vendidas",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "+$${String.format("%.2f", p.ganancia)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = ForestSuccess
                                                )
                                                Text(
                                                    text = "${p.margen}% margen",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Causes of Loss Table
                if (causesOfLoss.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Análisis de Causas de Merma",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    causesOfLoss.forEach { c ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = c.razon,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "${c.count} ocurrencias",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "-$${String.format("%.2f", c.totalCost)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = CrimsonDanger
                                                )
                                                Text(
                                                    text = "${c.percentage}% del total",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MOVIMIENTOS AUDITORIA
            var filterTipo by remember { mutableStateOf("Todos") }
            val tipos = listOf("Todos", "Venta", "Merma", "Ajuste")

            val filteredMovimientos = remember(movimientos, filterTipo) {
                if (filterTipo == "Todos") movimientos
                else movimientos.filter { it.tipo.equals(filterTipo, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(tipos) { t ->
                            val isSelected = t == filterTipo
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { filterTipo = t }
                            ) {
                                Text(
                                    text = t,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredMovimientos.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No hay movimientos registrados",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMovimientos, key = { it.id }) { mov ->
                        MovimientoItemCard(movimiento = mov)
                    }
                }
            }
        }
    }
}

data class RankedProduct(
    val nombre: String,
    val categoria: String,
    val unidades: Double,
    val ganancia: Double,
    val margen: Int
)

data class LossCause(
    val razon: String,
    val count: Int,
    val totalCost: Double,
    val percentage: Int
)

@Composable
fun MovimientoItemCard(
    movimiento: MovimientoEntity,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(movimiento.fecha) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(movimiento.fecha))
    }

    val (badgeBg, badgeFg, icon) = when (movimiento.tipo.lowercase()) {
        "venta" -> Triple(Color(0xFFE8F5E9), ForestSuccess, Icons.Default.PointOfSale)
        "merma" -> Triple(Color(0xFFFFEBEE), CrimsonDanger, Icons.Default.TrendingDown)
        else -> Triple(Color(0xFFE3F2FD), SteelSecondary, Icons.Default.SwapVert)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = movimiento.productoNombre,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${if (movimiento.cantidad > 0) "+" else ""}${movimiento.cantidad}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (movimiento.cantidad >= 0) ForestSuccess else CrimsonDanger
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = movimiento.referencia,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
