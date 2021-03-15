package me.eternal.engine

data class GameLog(
    val setupLog: SetupLog,
    val state: GameState = GameState(setupLog, 0, false),
    val events: List<GameEvent> = listOf()
) {
    fun receive(vararg events: GameEvent) = events.foldRight(this) { event, gameLog ->
        gameLog.receive(event)
    }


    fun receive(event: GameEvent) = GameLog(setupLog, state.receive(event), events.plus(event))

    fun projection(actions: List<GameAction> = listOf()): GameLogProjection = GameLogProjection(this, actions)

    fun eventSummary(): String {
        fun resolve(playerId: PlayerId, cardIdx: Int): String {
            val draw = setupLog.playerSetups.getValue(playerId).last()
            val card = draw.deck.cards.find { it.idx == cardIdx }
            return card?.card?.toString() ?: draw.hand.cards.getValue(cardIdx).card.toString()
        }
        return events.map {
            when (it) {
                is GameEvent.PowerReplenished -> ""
                is GameEvent.CardDrawn -> "Draw ${resolve(it.playerId, it.cardIdx)} by ${it.playerId}"
                is GameEvent.CardPlayed -> "Play ${resolve(it.playerId, it.cardIdx)} by ${it.playerId}"
                else -> it.toString()
            }
        }.filterNot { it.isBlank() }.joinToString(separator = "\n")
    }

    fun currentPlayer() = state.currentPlayer()
    fun night() = state.night
    fun playersOrder() = setupLog.playersOrder(currentPlayer())
    fun players() = state.players
    fun playersInOrder() = setupLog.playersOrder(currentPlayer()).map { it to state.player(it) }
    fun player(playerId: PlayerId): PlayerState = state.player(playerId)
    fun card(playerId: PlayerId, cardIdx: Int): Card? = player(playerId).hand.get(cardIdx)
}

data class GameLogProjection(val gameLog: GameLog, val upcomingActions: List<GameAction> = listOf()) {
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

    fun playersOrder(currentPlayer: PlayerId): List<PlayerId> {
        val idx = order.indexOf(currentPlayer)
        return if (idx + 1 == order.size) {
            order
        } else {
            order.subList(idx + 1, order.size).plus(order.subList(0, idx + 1))
        }
    }

}