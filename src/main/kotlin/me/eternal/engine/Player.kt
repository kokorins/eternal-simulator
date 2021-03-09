package me.eternal.engine

import me.eternal.decks.Deck
import me.eternal.decks.DeckLibrary
import me.eternal.decks.Hand
import me.eternal.decks.LocalLibrary
import me.eternal.players.DeciderLibrary
import me.eternal.players.LocalPlayers

interface Decider {
    val me: PlayerId
    fun muligan(order: Int, hand: Hand, muliganCount: Int): Boolean = true
    fun act(log: GameLogProjection): PlayerRequest?
    fun decide(decision: Decision, log: GameLogProjection,
               default: (PlayerId, GameLogProjection) -> List<PlayerRequest>): List<PlayerRequest> = default(me, log)
}

inline class PlayerId(val id: Int) {
    override fun toString(): String {
        return "$id"
    }
}

data class Player(val name: String, val deck: Deck, val decider: Decider) : Decider by decider

interface PlayerProvider {
    fun create(playerId: PlayerId, player: String, deck: String): Player
}

object DefaultPlayerProvider : PlayerProvider {
    private val deckLibrary: DeckLibrary = LocalLibrary
    private val playerLibrary: DeciderLibrary = LocalPlayers
    override fun create(playerId: PlayerId, player: String, deck: String): Player {
        return Player(player, deckLibrary.get(deck), playerLibrary.get(player, playerId))
    }
}