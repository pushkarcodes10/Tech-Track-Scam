package com.example.screentimetracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SessionRepository(context: Context) {

    private val dao: SessionDao = AppDatabase.getInstance(context).sessionDao()

    val allSessions: Flow<List<SessionEntity>> = dao.getAllSessions()

    suspend fun insert(session: SessionEntity): Long = dao.insert(session)
}
