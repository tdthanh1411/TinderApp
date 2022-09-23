package com.twilio.conversation.data.model.localCache.dao

import androidx.room.*
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantsDao {

    // Get all Participants for conversation
    @Query("SELECT * FROM participant_table WHERE conversationSid = :conversationSid ORDER BY friendlyName")
    fun getAllParticipants(conversationSid: String): Flow<List<ParticipantDataItem>>

    // Get all Participants for conversation who are typing
    @Query("SELECT * FROM participant_table WHERE conversationSid = :conversationSid AND typing")
    fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(participant: ParticipantDataItem)

    @Delete
    fun delete(participant: ParticipantDataItem)
}
