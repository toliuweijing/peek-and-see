package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RoutineBuilderScreen
import com.example.ui.screens.TrainingTimerScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import com.example.viewmodel.TrainingState

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Request permission to post notifications (Android 13+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
    }
    
    // Core database components initialization
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = AppRepository(database.appDao())
    val factory = MainViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppNavHost(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun MainAppNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val trainingState by viewModel.trainingState.collectAsStateWithLifecycle()

    // Smooth state-driven transitions for timer lifecycle (Idle -> Training -> Summary)
    LaunchedEffect(trainingState) {
        when (trainingState) {
            TrainingState.RUNNING -> {
                if (navController.currentDestination?.route != "training") {
                    navController.navigate("training") {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
            TrainingState.COMPLETED -> {
                if (navController.currentDestination?.route != "summary") {
                    navController.navigate("summary") {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
            TrainingState.IDLE -> {
                if (navController.currentDestination?.route != "dashboard") {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onStartTraining = { routine ->
                    viewModel.startSession(routine)
                },
                onNavigateToCreateRoutine = {
                    navController.navigate("create_routine")
                },
                onNavigateToEditRoutine = { routineId ->
                    navController.navigate("edit_routine/$routineId")
                }
            )
        }

        composable("create_routine") {
            RoutineBuilderScreen(
                viewModel = viewModel,
                routineId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "edit_routine/{routineId}",
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId")
            RoutineBuilderScreen(
                viewModel = viewModel,
                routineId = routineId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("training") {
            TrainingTimerScreen(
                viewModel = viewModel
            )
        }

        composable("summary") {
            SummaryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}

