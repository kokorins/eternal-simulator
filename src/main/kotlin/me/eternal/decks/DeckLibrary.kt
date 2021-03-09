package me.eternal.decks

import me.eternal.engine.JustCard
import me.eternal.engine.JustPower
import me.eternal.engine.SeekPower

interface DeckLibrary {
    fun get(deckName: String): Deck
}

object LocalLibrary : DeckLibrary {
    override fun get(deckName: String): Deck {
        return decks.getValue(deckName)
    }

    private fun empty75(): Pair<String, Deck> {
        val deck = Deck.of("empty-75") {
            +(25.of(JustPower))
            +(50.of(JustCard(0)))
        }
        return deck.name to deck
    }

    private fun emptyWithSeek(): Pair<String, Deck> {
        val deck = Deck.of("empty-with-seek") {
            +(25.of(JustPower))
            +(4.of(SeekPower))
            +(46.of(JustCard(0)))
        }
        return deck.name to deck
    }

    private val decks = mapOf(empty75(), emptyWithSeek())
}