package me.eternal.players

import me.eternal.engine.EndGameRequest
import me.eternal.engine.GameLogProjection
import me.eternal.engine.PlayerId
import me.eternal.engine.PlayerRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface PlayerEngine {
    fun action(log: GameLogProjection): PlayerRequest?
}

class TurnLimiter(val playerId: PlayerId, val numTurns: Int = 10) : PlayerEngine {
    override fun action(log: GameLogProjection): PlayerRequest? {
        return if (log.gameLog.state.turn >= numTurns) {
            if (log.upcomingActions.isEmpty()) {
                logger.info("End Requested on turn ${log.gameLog.state.turn}.")
                EndGameRequest(playerId)
            } else {
                null
            }
        } else {
            null
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TurnLimiter::class.simpleName)
    }
}