package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.ListenerRegistration
import com.wepool.app.data.model.chat.ChatMessage

interface IRideChatRepository {
    suspend fun sendGroupMessage(rideId: String, message: ChatMessage)
    suspend fun sendPrivateMessage(rideId: String, passengerId: String, message: ChatMessage)
    fun listenToGroupChat(rideId: String, onUpdate: (List<ChatMessage>) -> Unit): ListenerRegistration
    fun listenToPrivateChat(rideId: String, passengerId: String, onUpdate: (List<ChatMessage>) -> Unit): ListenerRegistration
    fun removeListener(registration: ListenerRegistration)
}