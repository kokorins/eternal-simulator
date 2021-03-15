package me.eternal.engine

/**
 * The request from player, which after the validation should be similar to GameAction
 */
interface PlayerRequest {
    fun toAction(): GameAction
}

data class DiscardRequest(val playerId: PlayerId, val cardIdx: Int) : PlayerRequest {
    override fun toAction(): GameAction = DiscardCard(playerId, cardIdx)
}

data class DrawCardRequest(val playerId: PlayerId, val cardIdx: Int) : PlayerRequest {
    override fun toAction(): GameAction = TutorCard(playerId, cardIdx)
}

data class PlayCardRequest(val playerId: PlayerId, val cardIdx: Int) : PlayerRequest {
    override fun toAction(): GameAction = PlayCard(playerId, cardIdx)
}

data class EndGameRequest(val playerId: PlayerId) : PlayerRequest {
    override fun toAction(): GameAction = EndGame(playerId)
}
