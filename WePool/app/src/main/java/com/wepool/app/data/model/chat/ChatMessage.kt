package com.wepool.app.data.model.chat

import com.google.firebase.Timestamp

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val message: String = ""
)