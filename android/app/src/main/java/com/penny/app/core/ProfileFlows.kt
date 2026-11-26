package com.penny.app.core

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Profile Settings (profile_settings.dart) and Edit Profile (edit_profile.dart)
 * decisions and their user-visible texts, independent of Compose.
 */
object ProfileFlows {

    sealed interface Outcome {
        /** Success: close the dialog / navigate as the page does; show [message]. */
        data class Done(val message: String) : Outcome
        /** Stay put and show [message]. */
        data class Failed(val message: String) : Outcome
        /** Nothing visible. */
        data object Silent : Outcome
    }

    /**
     * _changePassword (:91-125): mismatch is checked first (dialog stays
     * open); then re-auth + update; any error → "Failed to change password: $e".
     * The dialog's controllers are never cleared (reopening keeps the text).
     */
    suspend fun changePassword(auth: AuthService, current: String, new: String, confirm: String): Outcome {
        if (new != confirm) return Outcome.Failed("New passwords do not match")
        return try {
            auth.changePassword(current, new)
            Outcome.Done("Password changed successfully")
        } catch (e: AuthFailure) {
            Outcome.Failed("Failed to change password: ${e.dartText}")
        }
    }

    /** Storage `refFromURL(url).delete()` after the backend already removed the blob (mimicked). */
    /**
     * v2.1 delete flow (artboard S02): the profile doc, claims, auth account
     * and prefs go, then Sign In. The reauthentication happens in the modal
     * before this runs.
     *
     * LEDGER DECISION (redesign): the v1 flow reproduced Flutter's bug where
     * deleting with a profile picture loaded threw object-not-found AFTER the
     * server data was gone, leaving a half-deleted account. The v2.1
     * redesign implements deletion as designed — the quirk is retired.
     */
    suspend fun deleteAccount(
        auth: AuthService,
        directory: UserDirectory,
        prefs: UserPrefs,
        username: String?,
    ): Outcome = try {
        val user = (auth.state.value as? AuthState.SignedIn)?.user
        if (user == null || username == null) throw AuthFailure("Exception: No user currently signed in")
        if (!directory.deleteUser(username)) throw AuthFailure("Exception: Failed to delete account on server")
        directory.deleteCustomClaims(user.uid)
        auth.deleteCurrentUser()
        AuthFlows.signOut(auth, prefs)
        Outcome.Done("Account deleted successfully")
    } catch (e: AuthFailure) {
        Outcome.Failed("An error occurred while deleting account: ${e.dartText}")
    } catch (e: Exception) {
        // Dart `catch (e)` catches everything (012 review): transport errors too.
        Outcome.Failed("An error occurred while deleting account: $e")
    }

    /** Edit Profile prefill (:83-130): `userData?['x'] ?? ''`; any non-string → all empty. */
    data class Fields(val firstName: String, val lastName: String, val email: String, val phone: String)

    fun fields(doc: JsonObject?): Fields {
        val user = doc?.get("user") as? JsonObject ?: return Fields("", "", "", "")
        val keys = listOf("firstName", "lastName", "email", "phone")
        val values = keys.map { user[it] }
        // Map<String, String?> literal: a non-null non-String value is a TypeError → caught → all null.
        if (values.any { it != null && !(it is JsonPrimitive && (it.isString || it.content == "null")) }) {
            return Fields("", "", "", "")
        }
        val text = values.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content ?: "" }
        return Fields(text[0], text[1], text[2], text[3])
    }

    /** The read-only username field: `username ?? ' '` (a single space). */
    fun usernameField(username: String?): String = username ?: " "

    /** ToastMessages kinds (consts/toast_messages.dart). */
    enum class Toast { SUCCESS, ERROR, INFO, WARNING }

    sealed interface SaveResult {
        /** All four toasts fire, every one saying the same thing (:203-206). */
        data class Toasts(val toasts: List<Pair<Toast, String>>) : SaveResult
        data class SnackBar(val message: String) : SaveResult
        data object Silent : SaveResult
    }

    /**
     * "Save Changes" (:189-224 + :398-401): PUT the profile, then update the
     * claims (fullName "first last") and fire the four toasts; failures →
     * SnackBar. Regardless of the outcome, `saveFullName("first last")` runs
     * afterwards (even when no username was found or the update failed).
     */
    suspend fun saveProfile(
        auth: AuthService,
        directory: UserDirectory,
        prefs: UserPrefs,
        username: String?,
        fields: Fields,
    ): SaveResult {
        val fullName = "${fields.firstName} ${fields.lastName}"
        val result = if (username == null) {
            SaveResult.Silent
        } else {
            try {
                if (!directory.updateUser(username, fields.firstName, fields.lastName, fields.email, fields.phone)) {
                    throw AuthFailure("Exception: Failed to update profile")
                }
                val user = (auth.state.value as? AuthState.SignedIn)?.user
                when {
                    user == null -> SaveResult.Silent
                    directory.updateCustomClaims(user.uid, username, fullName) ->
                        SaveResult.Toasts(Toast.entries.map { it to "Profile Updated Successfully" })
                    else -> throw AuthFailure("Exception: Failed to update custom claims: ")
                }
            } catch (e: AuthFailure) {
                SaveResult.SnackBar("An error occurred while updating profile: ${e.dartText}")
            } catch (e: Exception) {
                SaveResult.SnackBar("An error occurred while updating profile: $e")
            }
        }
        prefs.saveFullName(fullName)
        return result
    }

    /** _updateProfilePicture (:226-260): cancelled pick / no username → nothing. */
    suspend fun updatePicture(images: ProfileImages, username: String?, picked: String?): Outcome {
        if (picked == null || username == null) return Outcome.Silent
        return try {
            if (images.upload(username, picked)) Outcome.Done("Profile picture updated successfully")
            else Outcome.Failed("Failed to update profile picture")
        } catch (e: Exception) {
            Outcome.Failed("An error occurred while updating profile picture")
        }
    }

    /** _deleteProfilePicture (:262-290): any 200 — even "No profile picture found" — is success. */
    suspend fun deletePicture(images: ProfileImages, username: String?): Outcome {
        if (username == null) return Outcome.Silent
        return try {
            if (images.delete(username)) Outcome.Done("Profile picture deleted successfully")
            else Outcome.Failed("Failed to delete profile picture")
        } catch (e: Exception) {
            Outcome.Failed("An error occurred while deleting profile picture")
        }
    }
}
