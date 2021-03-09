package me.eternal.decks

import me.eternal.engine.*

inline class CardId(val cardId: String)

interface CardLibrary {
    fun get(cardId: CardId): Card

    class Builder {
        val cards = mutableListOf<Card>()
        operator fun Card.unaryPlus() {
            cards.add(this)
        }

        fun <T> allOf(items: Iterable<T>, block: (T) -> Card) {
            cards.addAll(items.map(block))
        }

        fun build(): Map<CardId, Card> = cards.associateBy { it.id }
    }
}

object FullLibrary : CardLibrary {
    override fun get(cardId: CardId): Card {
        return cards.getValue(cardId)
    }

    private fun register(block: CardLibrary.Builder.() -> Unit): Map<CardId, Card> {
        val builder = CardLibrary.Builder()
        block(builder)
        return builder.build()
    }

    private val cards = register {
        +JustPower
        +JustCard(0)
        +SeekPower
        allOf(Influence.values().asIterable()) {
            Sigil(it)
        }
    }
}