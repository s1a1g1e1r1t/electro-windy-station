package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.screens.BlueprintsScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.InstructionsScreen
import com.example.ui.screens.MaterialsScreen
import com.example.ui.theme.VitroDimTheme
import kotlin.math.roundToInt

const val ROUTE_CALCULATOR = "calculator"
const val ROUTE_BLUEPRINTS = "blueprints"
const val ROUTE_INSTRUCTIONS = "instructions"
const val ROUTE_MATERIALS = "materials"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val appDestinations = listOf(
    NavDestination(ROUTE_CALCULATOR, R.string.nav_calculator, Icons.Filled.Calculate, Icons.Outlined.Calculate),
    NavDestination(ROUTE_BLUEPRINTS, R.string.nav_blueprints, Icons.Filled.Architecture, Icons.Outlined.Architecture),
    NavDestination(ROUTE_INSTRUCTIONS, R.string.nav_instructions, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    NavDestination(ROUTE_MATERIALS, R.string.nav_materials, Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitroDimTheme {
                VitroDimApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitroDimApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CALCULATOR

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${state.inputs.roomAreaSqm.roundToInt()} м² • Ø${state.inputs.rotorDiameterM}м",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        appDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        appDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }

                AppNavHost(
                    navController = navController,
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    state: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_CALCULATOR,
        modifier = modifier
    ) {
        composable(ROUTE_CALCULATOR) {
            CalculatorScreen(
                state = state,
                onRoomAreaChange = viewModel::updateRoomArea,
                onCeilingHeightChange = viewModel::updateCeilingHeight,
                onInsulationChange = viewModel::updateInsulation,
                onHeatingModeChange = viewModel::updateHeatingMode,
                onTurbineTypeChange = viewModel::updateTurbineType,
                onRotorDiameterChange = viewModel::updateRotorDiameter,
                onMastHeightChange = viewModel::updateMastHeight,
                onTerrainChange = viewModel::updateTerrain,
                onWinterDesignToggle = viewModel::setWinterDesignConditions,
                onManualWindChange = viewModel::setManualWindOverride,
                onAutoSizeForRoom = viewModel::autoSizeForRoom,
                onSelectLocation = viewModel::selectLocationPreset,
                onDetectGps = viewModel::detectGpsLocation,
                onRefreshWeather = viewModel::refreshWeather,
                onClearGpsMessage = viewModel::clearGpsMessage
            )
        }
        composable(ROUTE_BLUEPRINTS) {
            BlueprintsScreen(state = state)
        }
        composable(ROUTE_INSTRUCTIONS) {
            InstructionsScreen(
                state = state,
                onToggleStepCompleted = viewModel::toggleStepCompleted
            )
        }
        composable(ROUTE_MATERIALS) {
            MaterialsScreen(
                state = state,
                onToggleMaterial = viewModel::toggleMaterialChecked,
                onClearChecked = viewModel::clearCheckedMaterials,
                onBuildShareText = viewModel::buildShareableMaterialsText
            )
        }
    }
}
