package me.eternal.engine

/**
 * An special action required from player, there always should be default fallback
 */
interface Decision {
    val byPlayer: PlayerId
    fun default(log: GameLogProjection): List<PlayerRequest>
    fun make(log: GameLogProjection, players: Map<PlayerId, Player>): List<PlayerRequest> =
        players.getValue(byPlayer).decider.decide(this, log, this::default)
}

data class DrawSigilDecision(override val byPlayer: PlayerId) : Decision {
    override fun default(log: GameLogProjection): List<PlayerRequest> {
        val power = log.player(byPlayer).deck.find { it.power() }.firstOrNull()
        return if (power != null)
            listOf(DrawCardRequest(byPlayer, power.idx))
        else
            listOf()
    }
}

data class DiscardDecision(override val byPlayer: PlayerId, val numCards: Int) : Decision {
    override fun default(log: GameLogProjection): List<PlayerRequest> {
        val player = log.gameLog.state.player(byPlayer)
        val cardIndices = player.hand.cards.keys.take(numCards)
        return cardIndices.map { DiscardRequest(byPlayer, it) }
    }
}