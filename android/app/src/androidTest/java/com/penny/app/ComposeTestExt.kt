package com.penny.app

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText

fun ComposeContentTestRule.onAllNodesWithTagExists(tag: String): Boolean =
    onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

fun ComposeContentTestRule.onAllNodesWithTextExists(text: String, substring: Boolean = false): Boolean =
    onAllNodesWithText(text, substring = substring, useUnmergedTree = substring).fetchSemanticsNodes().isNotEmpty()
