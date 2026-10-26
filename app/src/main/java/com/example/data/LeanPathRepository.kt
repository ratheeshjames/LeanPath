package com.example.data

import com.example.domain.MetabolicEngine
import com.example.model.LeanPathState
import com.example.model.ProfileMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MultiProfileSnapshot(
    val activeKey: String,
    val activeName: String,
    val profiles: List<ProfileMeta>,
    val state: LeanPathState
)

class LeanPathRepository(private val dao: ProfileStateDao) {

    val snapshotFlow: Flow<MultiProfileSnapshot?> = dao.observeAllProfiles().map { list ->
        if (list.isEmpty()) {
            null
        } else {
            val active = list.firstOrNull { it.isActive } ?: list.first()
            MultiProfileSnapshot(
                activeKey = active.profileKey,
                activeName = active.displayName,
                profiles = list.map { ProfileMeta(key = it.profileKey, name = it.displayName) },
                state = MetabolicEngine.deserializeStateFromJson(active.stateJson)
            )
        }
    }

    suspend fun saveProfileState(
        profileKey: String,
        displayName: String,
        state: LeanPathState,
        isActive: Boolean = true
    ) {
        val json = MetabolicEngine.serializeStateToJson(state)
        dao.upsertProfile(
            ProfileStateEntity(
                profileKey = profileKey,
                displayName = displayName,
                isActive = isActive,
                stateJson = json,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (isActive) {
            dao.setActiveProfile(profileKey)
        }
    }

    suspend fun switchProfile(profileKey: String) {
        dao.setActiveProfile(profileKey)
    }

    suspend fun createNewProfile(displayName: String): String {
        val key = "leanpath.p${System.currentTimeMillis()}"
        val emptyState = LeanPathState(onboarded = false)
        saveProfileState(
            profileKey = key,
            displayName = displayName,
            state = emptyState,
            isActive = true
        )
        return key
    }

    suspend fun deleteProfile(profileKey: String) {
        dao.deleteProfileByKey(profileKey)
    }
}
