package com.example.taptalk.data

import com.google.mlkit.nl.smartreply.TextMessage

class ConversationManager {
    private val messages = mutableListOf<TextMessage>()

    fun addLocalMessage(text: String) {
        messages.add(TextMessage.createForLocalUser(text, System.currentTimeMillis()))
    }

    fun addRemoteMessage(text: String, userId: String = "other") {
        messages.add(TextMessage.createForRemoteUser(text, System.currentTimeMillis(), userId))
    }

    fun getHistory(): List<TextMessage> = messages.toList()

    fun clear() = messages.clear()
}
