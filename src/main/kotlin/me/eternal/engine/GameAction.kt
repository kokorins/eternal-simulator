package me.eternal.engine

/**
 * Intention that should happen
 */
interface GameAction {
    fun apply(log: GameLog): List<GameEvent>
}

data class TurnStart(val playerId: PlayerId) : GameAction {
    override fun apply(log: GameLog): List<GameEvent> {
        return listOf(GameEvent.TurnStart(playerId), GameEvent.ReplenishPower(playerId))
    }
}

data class TurnEnd(val rules: Rules, val players: Map<PlayerId, Player>) : GameAction {
    override fun apply(log: GameLog): List<GameEvent> {
        return listOf(GameEvent.EndTurn(log.state.currentPlayer()))
    }

    override fun toString() = "TurnEnd"
}

data class DrawACard(val playerId: PlayerId) : GameAction {
    override fun apply(log: GameLog): List<GameEvent> {
        val gameState = log.state
        val player = gameState.players.getValue(playerId)
        val cardIdx = player.deck.top()
        return if (cardIdx != null)
            listOf(GameEvent.DrawCard(playerId, cardIdx))
        else {
            listOf(GameEvent.EndGame(playerId))
        }
    }
}

/**
 * An special action required from player, there always should be default fallback
 */
interface Decision {
    fun default(playerId: PlayerId, log: GameLogProjection): List<PlayerRequest>
    fun make(playerId: PlayerId, log: GameLogProjection, players: Map<PlayerId, Player>): List<PlayerRequest> =
            players.getValue(playerId).decider.decide(this, log, this::default)
}

object DrawSigilDecision : Decision {
    override fun default(playerId: PlayerId, log: GameLogProjection): List<PlayerRequest> {
        val powers = log.gameLog.state.player(playerId).deck.find { it.power() }
        val power = powers.firstOrNull()
        return if (power != null)
            listOf(DrawCardRequest(playerId, power.idx))
        else
            listOf()
    }
}

data class DiscardDecision(val numCards: Int) : Decision {
    override fun default(playerId: PlayerId, log: GameLogProjection): List<PlayerRequest> {
        val player = log.gameLog.state.player(playerId)
        val cardIdxs = player.hand.cards.keys.take(numCards)
        return cardIdxs.map { DiscardRequest(playerId, it) }
    }
}
