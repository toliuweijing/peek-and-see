package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.DashboardScreen
import com.example.viewmodel.MainViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MainViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()
        val application = context as Application
        val database = AppDatabase.getDatabase(context)
        val repository = AppRepository(database.appDao())
        
        // Reset shared preferences to ensure clean state
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        
        viewModel = MainViewModel(application, repository)
    }

    @Test
    fun bannerDismissalAndReappearanceTest() {
        // Start compose with DashboardScreen
        composeTestRule.setContent {
            DashboardScreen(
                viewModel = viewModel,
                onStartTraining = {},
                onNavigateToCreateRoutine = {},
                onNavigateToEditRoutine = {},
                onNavigateToHistoryManagement = {},
                onNavigateToReliabilitySetup = {}
            )
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // 1. Force isReliable = true using Reflection on the MainViewModel StateFlows
        setViewModelReliability(true)
        composeTestRule.waitForIdle()

        // Verify the dismiss button is displayed and the banner shows "Optimized" state
        composeTestRule.onNodeWithTag("dismiss_optimized_banner_btn", useUnmergedTree = true).assertIsDisplayed()

        // 2. Dismiss the banner
        composeTestRule.onNodeWithTag("dismiss_optimized_banner_btn", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        
        // 3. Verify it is no longer shown
        composeTestRule.onNodeWithTag("dismiss_optimized_banner_btn", useUnmergedTree = true).assertDoesNotExist()
        // And the card itself shouldn't be there - the resolve btn shouldn't be there either
        composeTestRule.onNodeWithTag("reliability_setup_banner_btn", useUnmergedTree = true).assertDoesNotExist()

        // 4. Emulate a reliability failure (e.g. notifications disabled)
        setViewModelReliability(false)
        composeTestRule.waitForIdle()
        
        // Let's assert the view model actually updated
        org.junit.Assert.assertFalse(viewModel.isNotificationsEnabled.value)

        // Give it a tiny bit of time for coroutines
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Debug print the tree
        composeTestRule.onRoot().printToLog("UI_TREE")

        // 5. Verify the banner re-appears in "Warning/Resolve" state and is not dismissed
        composeTestRule.onNodeWithTag("reliability_setup_banner_btn", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        // The resolve button is there, but dismiss is not there because it's not reliable
        composeTestRule.onNodeWithTag("dismiss_optimized_banner_btn", useUnmergedTree = true).assertDoesNotExist()

        // 6. Fix the reliability issue
        setViewModelReliability(true)
        composeTestRule.waitForIdle()

        // 7. Verify the banner is still visible and has the dismiss button again 
        // (because when it became unreliable, the dismissed state was wiped out)
        composeTestRule.onNodeWithTag("dismiss_optimized_banner_btn", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    private fun setViewModelReliability(isReliable: Boolean) {
        // Use reflection to modify the MutableStateFlows in MainViewModel since we don't have mock framework
        val notifField = MainViewModel::class.java.getDeclaredField("_isNotificationsEnabled").apply { isAccessible = true }
        val alarmField = MainViewModel::class.java.getDeclaredField("_isExactAlarmEnabled").apply { isAccessible = true }
        val batteryField = MainViewModel::class.java.getDeclaredField("_isBatteryUnrestricted").apply { isAccessible = true }

        val notifFlow = notifField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        val alarmFlow = alarmField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        val batteryFlow = batteryField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>

        notifFlow.value = isReliable
        alarmFlow.value = isReliable
        batteryFlow.value = isReliable
    }
}
