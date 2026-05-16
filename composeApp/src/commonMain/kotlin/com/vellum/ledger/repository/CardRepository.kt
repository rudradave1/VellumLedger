package com.vellum.ledger.repository

import com.vellum.ledger.database.LedgerDatabase
import com.vellum.ledger.domain.LedgerCard
import com.vellum.ledger.domain.CardType
import com.vellum.ledger.data.newLedgerId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepository(private val database: LedgerDatabase) {
    
    val cards: Flow<List<LedgerCard>> = database.state.map { it.cards }

    suspend fun addCard(
        name: String,
        number: String,
        type: CardType,
        expiry: String,
        balance: Long,
        color: String
    ) {
        val card = LedgerCard(
            id = newLedgerId(),
            cardName = name,
            cardNumber = number.takeLast(4),
            cardType = type,
            expiry = expiry,
            balance = balance,
            hexColor = color
        )
        database.insertCard(card)
    }

    suspend fun deleteCard(id: String) {
        database.deleteCard(id)
    }
}
