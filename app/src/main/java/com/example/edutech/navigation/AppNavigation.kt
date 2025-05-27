package com.example.edutech.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.edutech.ui.screens.*
import com.example.edutech.viewmodel.AuthViewModel
import com.example.edutech.viewmodel.GradeViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Subjects : Screen("subjects")
    object Grades : Screen("grades/{subjectId}") {
        fun createRoute(subjectId: String) = "grades/$subjectId"
    }
    object AddSubject : Screen("add_subject")
    object AddGrade : Screen("add_grade/{subjectId}") {
        fun createRoute(subjectId: String) = "add_grade/$subjectId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSubjects = { navController.navigate(Screen.Subjects.route) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Subjects.route) {
            SubjectsScreen(
                onNavigateToGrades = { subjectId ->
                    navController.navigate(Screen.Grades.createRoute(subjectId))
                },
                onNavigateToAddSubject = {
                    navController.navigate(Screen.AddSubject.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Grades.route) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
            GradesScreen(
                subjectId = subjectId,
                onNavigateToAddGrade = {
                    navController.navigate(Screen.AddGrade.createRoute(subjectId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AddSubject.route) {
            AddSubjectScreen(
                onSubjectAdded = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("add_grade/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            if (subjectId != null) {
                val viewModel: GradeViewModel = viewModel()
                AddGradeScreen(
                    subjectId = subjectId,
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
    }
} 