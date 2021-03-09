package me.eternal.players

import me.eternal.engine.Decision
import me.eternal.engine.DiscardDecision
import me.eternal.engine.DiscardRequest
import me.eternal.engine.GameLogProjection
import me.eternal.engine.PlayerId
import me.eternal.engine.PlayerRequest

interface SpecialEngine {
    fun make(decision: Decision, log: GameLogProjection): List<PlayerRequest>
}

class DiscardNonPowerFirst(val playerId: PlayerId) : SpecialEngine {
    override fun make(decision: Decision, log: GameLogProjection): List<PlayerRequest> {
        return when (decision) {
            is DiscardDecision -> {
                val player = log.gameLog.state.player(playerId)
                val hand = player.hand.nonPowers().plus(player.hand.powers())
                hand.take(decision.numCards).map { DiscardRequest(playerId, it.idx) }
            }
            else -> listOf()
        }
    }
}