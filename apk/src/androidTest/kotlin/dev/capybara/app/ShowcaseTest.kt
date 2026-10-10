package dev.capybara.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShowcaseTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun scanRequiresAReferenceBeforeCapture() {
        rule.onNodeWithText("Scan").performClick()
        rule.onNodeWithText("Choose the saved two-side reference to compare.").assertIsDisplayed()
        rule.onNodeWithText("Object name").assertDoesNotExist()
        rule.onNodeWithText("Capture side 1", substring = true).assertDoesNotExist()
        snapshot("scan-start")
    }

    @Test fun conciseShowcaseKeepsStatusInMenuAndRestoresSetup() {
        rule.onNodeWithText("V6").assertDoesNotExist()
        rule.onNodeWithText("Intelligence").assertIsNotDisplayed()
        rule.onNodeWithText("Enroll object").assertIsDisplayed()
        rule.onNodeWithText("Scan").assertIsDisplayed()
        snapshot("home")
        rule.onNodeWithContentDescription("Open menu").performClick()
        rule.onNodeWithText("Intelligence").assertIsDisplayed()
        snapshot("status")
        rule.onNodeWithText("Capture setup").performClick()
        rule.onNodeWithText("Object ID").performTextReplacement("ui-state-test-only")
        rule.onNodeWithText("Session ID").performTextReplacement("ui-test-session")
        rule.onNodeWithText("Open 4-position research capture").performScrollTo().performClick()
        rule.onNodeWithText("Capture response").assertIsDisplayed()
        rule.onNodeWithText("Empty case").performScrollTo()
        rule.onNodeWithText("Empty case").performClick()
        snapshot("capture")
        rule.activityRule.scenario.recreate()
        rule.onAllNodesWithText("ui-state-test-only")[0].assertIsDisplayed()
        rule.onNodeWithText("Empty case").assertIsSelected()
        rule.onNodeWithContentDescription("Open menu").performClick()
        rule.onNodeWithText("Capture setup").performClick()
        rule.onNodeWithText("ui-test-session").assertExists()
        rule.onNodeWithText("Close menu").performClick()
    }

    private fun snapshot(name: String) {
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(rule.activity.getExternalFilesDir(null), "capybara-$name.png")
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
