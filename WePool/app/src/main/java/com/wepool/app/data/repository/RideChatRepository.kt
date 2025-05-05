package com.wepool.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.wepool.app.data.model.chat.ChatMessage
import com.wepool.app.data.repository.interfaces.IRideChatRepository
import kotlinx.coroutines.tasks.await

class RideChatRepository(
    private val firestore: FirebaseFirestore
) : IRideChatRepository {

    override suspend fun sendGroupMessage(rideId: String, message: ChatMessage) {
        val docRef = firestore.collection("rides")
            .document(rideId)
            .collection("groupChat")
            .document()

        val msgWithId = message.copy(messageId = docRef.id)
        docRef.set(msgWithId).await()
    }

    override suspend fun sendPrivateMessage(rideId: String, passengerId: String, message: ChatMessage) {
        val docRef = firestore.collection("rides")
            .document(rideId)
            .collection("privateChats")
            .document(passengerId)
            .collection("messages")
            .document()

        val msgWithId = message.copy(messageId = docRef.id)
        docRef.set(msgWithId).await()
    }

    override fun listenToGroupChat(
        rideId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("rides")
            .document(rideId)
            .collection("groupChat")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    onUpdate(messages)
                }
            }
    }

    override fun listenToPrivateChat(
        rideId: String,
        passengerId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("rides")
            .document(rideId)
            .collection("privateChats")
            .document(passengerId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    onUpdate(messages)
                }
            }
    }

    override fun removeListener(registration: ListenerRegistration) {
        registration.remove()
    }
}

