package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ProfileExtras (frames 14–15, S02, S05): the dirty-form guard, inline
 * validation, the delete-account impact list, and CSV export.
 */
class ProfileExtrasTest {

    private val original = ProfileExtras.ProfileDraft(
        "Demo User", "demo@penny.app", "+91 98765 43210", ProfileExtras.PhotoStage.Unchanged,
    )

    @Test
    fun `the back guard fires on any edit, including a staged photo`() {
        assertFalse(ProfileExtras.isDirty(original, original))
        assertTrue(ProfileExtras.isDirty(original, original.copy(fullName = "Demo User Jr.")))
        assertTrue(ProfileExtras.isDirty(original, original.copy(email = "new@penny.app")))
        assertTrue(ProfileExtras.isDirty(original, original.copy(photo = ProfileExtras.PhotoStage.Removed)))
        assertTrue(ProfileExtras.isDirty(original, original.copy(photo = ProfileExtras.PhotoStage.Replaced("content://1"))))
    }

    @Test
    fun `save validation is top-down`() {
        assertNull(ProfileExtras.validate(original))
        assertEquals(ProfileExtras.SaveIssue.NameRequired, ProfileExtras.validate(original.copy(fullName = " ")))
        assertEquals(ProfileExtras.SaveIssue.EmailRequired, ProfileExtras.validate(original.copy(email = "")))
        assertEquals(ProfileExtras.SaveIssue.EmailInvalid, ProfileExtras.validate(original.copy(email = "not-an-email")))
    }

    @Test
    fun `the delete modal lists the exact data impact`() {
        val impact = ProfileExtras.deleteImpact(
            transactionCount = 18, monthLabel = "Nov", coins = 240, streakDays = 5,
            badgeCount = 2, unitCount = 1, hasPhoto = true, aiThreadCount = 2,
        )
        assertEquals(
            listOf(
                "18 Nov transactions and all other finance data",
                "Course progress, streaks, badges, and 240 coins",
                "Profile details and photo",
                "Penny AI threads",
            ),
            impact,
        )
        assertEquals(
            listOf("18 Nov transactions and all other finance data", "Profile details"),
            ProfileExtras.deleteImpact(18, "Nov", 0, 0, 0, 0, hasPhoto = false, aiThreadCount = 0),
        )
    }

    @Test
    fun `csv export quotes commas and doubles quotes`() {
        val csv = ProfileExtras.csv(
            listOf(
                ProfileExtras.CsvExpense("18 November", "Food & Dining", "Swiggy, family", "Card", 3850.0),
                ProfileExtras.CsvExpense("01 November", "Rent & Utilities", "Flat \"big\" rent", "UPI", 7500.0),
            ),
        )
        assertEquals(
            "Date,Category,Merchant,Payment,Amount\n" +
                "18 November,Food & Dining,\"Swiggy, family\",Card,3850.00\n" +
                "01 November,Rent & Utilities,\"Flat \"\"big\"\" rent\",UPI,7500.00\n",
            csv,
        )
    }
}
