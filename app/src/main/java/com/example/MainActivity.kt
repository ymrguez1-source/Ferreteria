package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ProductEntity
import com.example.ui.FerreteriaViewModel
import com.example.ui.components.AddMermaDialog
import com.example.ui.components.AddVentaDialog
import com.example.ui.components.AdjustStockDialog
import com.example.ui.components.ProductDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InformesScreen
import com.example.ui.screens.MermasScreen
import com.example.ui.screens.ProductosScreen
import com.example.ui.screens.UsuariosScreen
import com.example.ui.screens.VentasScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerracottaDark
import com.example.ui.theme.TerracottaPrimary

class MainActivity : ComponentActivity() {
    private val viewModel: FerreteriaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FerreteriaApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerreteriaApp(viewModel: FerreteriaViewModel) {
    var selectedScreen by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val ventas by viewModel.ventas.collectAsStateWithLifecycle()
    val mermas by viewModel.mermas.collectAsStateWithLifecycle()
    val movimientos by viewModel.movimientos.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLicenseActive by viewModel.isLicenseActive.collectAsStateWithLifecycle()
    val deviceId by viewModel.deviceId.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.productCategoryFilter.collectAsStateWithLifecycle()

    // Dialog states
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showVentaDialog by remember { mutableStateOf(false) }
    var showMermaDialog by remember { mutableStateOf(false) }
    var adjustingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    // Display snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ferretería",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = when (selectedScreen) {
                                0 -> "Panel Principal"
                                1 -> "Inventario"
                                2 -> "Ventas"
                                3 -> "Mermas"
                                4 -> "Informes"
                                else -> "Usuarios & Ajustes"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedScreen = 5 },
                        modifier = Modifier.testTag("btn_top_users")
                    ) {
                        BadgedBox(
                            badge = {
                                if (currentUser?.role == "administrador") {
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                        Text("A", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedScreen == 5) Icons.Default.ManageAccounts else Icons.Default.AccountCircle,
                                contentDescription = "Perfil y Usuarios",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TerracottaDark,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        indicatorColor = TerracottaPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_inicio")
                )
                NavigationBarItem(
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1 },
                    icon = {
                        val lowStock = products.count { it.stock <= 5 }
                        BadgedBox(
                            badge = {
                                if (lowStock > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$lowStock")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Productos")
                        }
                    },
                    label = { Text("Productos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        indicatorColor = TerracottaPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_productos")
                )
                NavigationBarItem(
                    selected = selectedScreen == 2,
                    onClick = { selectedScreen = 2 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Ventas") },
                    label = { Text("Ventas") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        indicatorColor = TerracottaPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_ventas")
                )
                NavigationBarItem(
                    selected = selectedScreen == 3,
                    onClick = { selectedScreen = 3 },
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Mermas") },
                    label = { Text("Mermas") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        indicatorColor = TerracottaPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_mermas")
                )
                NavigationBarItem(
                    selected = selectedScreen == 4,
                    onClick = { selectedScreen = 4 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Informes") },
                    label = { Text("Informes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaPrimary,
                        indicatorColor = TerracottaPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_informes")
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                0 -> DashboardScreen(
                    products = products,
                    ventas = ventas,
                    mermas = mermas,
                    onNavigateToVentas = { selectedScreen = 2 },
                    onNavigateToProductos = { selectedScreen = 1 },
                    onOpenAddVenta = { showVentaDialog = true },
                    onOpenAddProducto = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    onOpenAddMerma = { showMermaDialog = true }
                )

                1 -> ProductosScreen(
                    products = products,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setProductSearch(it) },
                    categoryFilter = categoryFilter,
                    onCategoryChange = { viewModel.setProductCategory(it) },
                    onAddProduct = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    onEditProduct = { p ->
                        editingProduct = p
                        showProductDialog = true
                    },
                    onAdjustStock = { p -> adjustingProduct = p },
                    onDeleteProduct = { p -> viewModel.deleteProduct(p.id) }
                )

                2 -> VentasScreen(
                    ventas = ventas,
                    onAddVenta = { showVentaDialog = true },
                    onDeleteVenta = { v -> viewModel.eliminarVenta(v.id) }
                )

                3 -> MermasScreen(
                    mermas = mermas,
                    onAddMerma = { showMermaDialog = true },
                    onDeleteMerma = { m -> viewModel.eliminarMerma(m.id) }
                )

                4 -> InformesScreen(
                    products = products,
                    ventas = ventas,
                    mermas = mermas,
                    movimientos = movimientos
                )

                5 -> UsuariosScreen(
                    currentUser = currentUser,
                    users = users,
                    deviceId = deviceId,
                    isLicenseActive = isLicenseActive,
                    onLogin = { u, p -> viewModel.login(u, p) },
                    onLogout = { viewModel.logout() },
                    onCreateUser = { u, p, r -> viewModel.createUser(u, p, r) },
                    onDeleteUser = { id -> viewModel.deleteUser(id) },
                    onActivateLicense = { key -> viewModel.activateLicense(key) },
                    onResetDemoData = { viewModel.resetDemoData() }
                )
            }
        }
    }

    // --- DIALOGS ---
    if (showProductDialog) {
        ProductDialog(
            initialProduct = editingProduct,
            onDismiss = {
                showProductDialog = false
                editingProduct = null
            },
            onSave = { nom, cat, cost, prec, un, stk ->
                viewModel.saveProduct(
                    id = editingProduct?.id ?: 0L,
                    nombre = nom,
                    categoria = cat,
                    costoUnitario = cost,
                    precioVenta = prec,
                    unidadMedida = un,
                    stock = stk
                )
                showProductDialog = false
                editingProduct = null
            }
        )
    }

    if (showVentaDialog) {
        AddVentaDialog(
            products = products,
            onDismiss = { showVentaDialog = false },
            onConfirm = { pid, cant, prec, cli ->
                viewModel.registrarVenta(pid, cant, prec, cli)
                showVentaDialog = false
            }
        )
    }

    if (showMermaDialog) {
        AddMermaDialog(
            products = products,
            onDismiss = { showMermaDialog = false },
            onConfirm = { pid, cant, raz, cost ->
                viewModel.registrarMerma(pid, cant, raz, cost)
                showMermaDialog = false
            }
        )
    }

    adjustingProduct?.let { prod ->
        AdjustStockDialog(
            product = prod,
            onDismiss = { adjustingProduct = null },
            onConfirm = { delta, motivo ->
                viewModel.ajustarStock(prod.id, delta, motivo)
                adjustingProduct = null
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

