package me.eternal.engine

import me.eternal.decks.*
import kotlin.random.Random

data class InitialDraw(val hand: Hand, val deck: OrderedDeck)

interface InitialDrawStrategy {
    fun extract(deck: Deck, random: Random): InitialDraw
}

class Uniform(private val library: CardLibrary, private val numCards: Int) : InitialDrawStrategy {
    override fun extract(deck: Deck, random: Random): InitialDraw {
        val cards = deck.cards.mapIndexed { idx, cardId -> OrderedCard(idx, library.get(cardId)) }
        val allCards = cards.shuffled(random)
        return InitialDraw(Hand(allCards.take(numCards).associateBy { it.idx }.toMutableMap()), OrderedDeck(allCards.drop(numCards).toMutableList()))
    }
}

class TwoThreeFourPower(private val library: CardLibrary, private val numCards: Int) : InitialDrawStrategy {
    override fun extract(deck: Deck, random: Random): InitialDraw {
        val cards = deck.cards.mapIndexed { idx, cardId -> OrderedCard(idx, library.get(cardId)) }
        val numPower = random.nextInt(3) + 2
        val numNonPower = numCards - numPower
        val (power, nonPower) = cards.shuffled(random).partition { it.isPower() != null }
        val hand = power.take(numPower).plus(nonPower.take(numNonPower))
        val deck = power.drop(numPower).plus(nonPower.drop(numNonPower)).shuffled(random)
        return InitialDraw(Hand(hand.associateBy { it.idx }.toMutableMap()), OrderedDeck(deck.toMutableList()))
    }
}