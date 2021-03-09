package me.eternal.engine

/**
 * The request from player, which after the validation should be similar to GameAction
 */
interface PlayerRequest : GameAction {
    fun apply(gameState: GameState): List<GameEvent>
    override fun apply(log: GameLog) = apply(log.state)
}

data class DiscardRequest(val playerId: PlayerId, val cardIdx: Int) : PlayerRequest {
    override fun apply(gameState: GameState): List<GameEvent> {
        return listOf(GameEvent.Discard(playerId, cardIdx))
    }
}

data class DrawCardRequest(val playerId: PlayerId, val cardIdx: Int) : PlayerRequest {
    override fun apply(gameState: GameState): List<GameEvent> {
        val player = gameState.player(playerId)
        return if (player.deck.cards.any { it.idx == cardIdx }) {
            listOf(GameEvent.DrawCard(playerId, cardIdx))
        } else {
            listOf()
        }
    }
}

data class PlayCardRequest(val playerId: PlayerId, val cardIdx: Int) : PlayerRequest {
    override fun apply(gameState: GameState): List<GameEvent> {
        val player = gameState.player(playerId)
        return if (player.canPlay(cardIdx))
            listOf(GameEvent.PlayCard(playerId, cardIdx))
        else
            listOf()
    }
}

data class EndTurnRequest(val playerId: PlayerId) : PlayerRequest {
    override fun apply(gameState: GameState): List<GameEvent> = listOf(GameEvent.EndTurn(playerId))
}

data class EndGameRequest(val playerId: PlayerId) : PlayerRequest {
    override fun apply(gameState: GameState): List<GameEvent> = listOf(GameEvent.EndGame(playerId))
}
