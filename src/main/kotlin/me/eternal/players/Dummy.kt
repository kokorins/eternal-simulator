package me.eternal.players

import me.eternal.engine.Decider
import me.eternal.engine.EndTurnRequest
import me.eternal.engine.GameLogProjection
import me.eternal.decks.Hand
import me.eternal.engine.PlayerRequest
import me.eternal.engine.PlayerId

data class Dummy(val playerId: PlayerId): Decider {
    override val me: PlayerId
        get() = playerId

    override fun muligan(order: Int, hand: Hand, muliganCount: Int) = false

    override fun act(log: GameLogProjection): PlayerRequest = EndTurnRequest(playerId)
}