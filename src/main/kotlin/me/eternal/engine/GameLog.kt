package me.eternal.engine

data class GameLog(
    val setupLog: SetupLog,
    val state: GameState = GameState(setupLog, 0, false),
    val events: List<GameEvent> = listOf()
) {
    fun receive(events: List<GameEvent>) = events.foldRight(this) { event, gameLog ->
        gameLog.receive(event)
    }

    fun receive(event: GameEvent) = GameLog(setupLog, state.receive(event), events.plus(event))

    fun projection(playerId: PlayerId, actions: List<GameAction>): GameLogProjection = GameLogProjection(this, actions)

    fun eventSummary(): String {
        fun resolve(playerId: PlayerId, cardIdx: Int): String {
            val draw = setupLog.playerSetups.getValue(playerId).last()
            val card = draw.deck.cards.find { it.idx == cardIdx }
            return card?.card?.toString() ?: draw.hand.cards.getValue(cardIdx).card.toString()
        }
        return events.map {
            when (it) {
                is GameEvent.ReplenishPower -> ""
                is GameEvent.DrawCard -> "Draw ${resolve(it.playerId, it.idx)} by ${it.playerId}"
                is GameEvent.PlayCard -> "Play ${resolve(it.playerId, it.idx)} by ${it.playerId}"
                else -> it.toString()
            }
        }.filterNot { it.isBlank() }.joinToString(separator = "\n")
    }
}

data class GameLogProjection(val gameLog: GameLog, val actions: List<GameAction> = listOf()) {
    fun state() = gameLog.state
    fun player(playerId: PlayerId) = gameLog.state.player(playerId)
}

data class SetupLog(val order: List<PlayerId>, val playerSetups: Map<PlayerId, List<InitialDraw>>) {
    private fun muliganSummary(): String {
        return playerSetups.map { (pId, draws) -> "$pId: ${draws.size}" }
            .joinToString(prefix = "(", postfix = ")") { it }
    }

    fun minimalSummary(): String {
        return "order $order ${muliganSummary()}"
    }

    fun orderStarting(currentPlayer: PlayerId): List<PlayerId> {
        val idx = order.indexOf(currentPlayer)
        return if (idx + 1 == order.size) {
            order
        } else {
            order.subList(idx + 1, order.size).plus(order.subList(0, idx + 1))
        }
    }

}