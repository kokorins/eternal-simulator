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
               default: (GameLogProjection) -> List<PlayerRequest>): List<PlayerRequest> = default(log)
}

inline class PlayerId(private val id: Int) {
    override fun toString(): String {
        return "$id"
    }
}

data class Player(val name: String, val deck: Deck, val decider: Decider) : Decider by decider

interface PlayerProvider {
    fun create(playerId: PlayerId, player: String, deck: String): Pair<PlayerId, Player>
}

object DefaultPlayerProvider : PlayerProvider {
    private val deckLibrary: DeckLibrary = LocalLibrary
    private val playerLibrary: DeciderLibrary = LocalPlayers
    override fun create(playerId: PlayerId, player: String, deck: String): Pair<PlayerId, Player> {
        return playerId to Player(player, deckLibrary.get(deck), playerLibrary.get(player, playerId))
    }
}