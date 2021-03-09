package me.eternal.players

import me.eternal.decks.Hand
import me.eternal.decks.OrderedCard

interface MuliganEngine {
    fun muligan(order: Int, hand: Hand, muliganCount: Int): Boolean
}

class HandCheck(val block: (Hand) -> Boolean) : MuliganEngine {
    override fun muligan(order: Int, hand: Hand, muliganCount: Int): Boolean {
        return block(hand)
    }

    companion object {
        fun power(block: (List<OrderedCard>) -> Boolean) = HandCheck { hand -> block(hand.powers()) }
        fun atLeast(lowerBound: Int) = { cards: List<OrderedCard> -> cards.size >= lowerBound }
    }
}