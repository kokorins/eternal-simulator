package me.eternal.decks

import me.eternal.engine.Card

data class Deck(val name: String, val cards: List<CardId>) {
    class Builder(private val name: String) {
        private val cards = mutableListOf<CardId>()
        operator fun Card.unaryPlus() {
            cards.add(id)
        }

        operator fun List<Card>.unaryPlus() {
            cards.addAll(this.map { it.id })
        }

        fun Int.of(card: Card) = List(this) { card }
        fun build() = Deck(name, cards.toList())
    }

    companion object {
        fun of(name: String, block: Builder.() -> Unit): Deck {
            val builder = Builder(name)
            block(builder)
            return builder.build()
        }
    }
}

data class Hand(val cards: Map<Int, OrderedCard>) {
    fun extract(cardIdx: Int): Pair<Hand, OrderedCard?> {
        val card = cards[cardIdx]
        return if (card != null) {
            Hand(cards.minus(cardIdx)) to card
        } else {
            this to null
        }
    }

    fun add(card: OrderedCard) = Hand(cards.plus(card.idx to card))

    fun select(predicate: (OrderedCard) -> Boolean): List<OrderedCard> = cards.values.filter(predicate)

    fun powers() = select { it.isPower() != null }
    fun nonPowers() = select { it.isPower() == null }
    fun get(cardIdx: Int) = cards[cardIdx]
}

data class OrderedCard(val idx: Int, val card: Card) : Card by card

data class OrderedDeck(val cards: List<OrderedCard>) {
    fun extract(cardIdx: Int): Pair<OrderedDeck, OrderedCard?> {
        val card = cards.find { it.idx == cardIdx }
        return if (card != null) {
            OrderedDeck(cards.minus(card)) to card
        } else
            this to null
    }

    fun find(pred: (OrderedCard) -> Boolean): Sequence<OrderedCard> {
        return cards.asSequence().filter(pred)
    }

    fun top(): Int? = cards.firstOrNull()?.idx
}

