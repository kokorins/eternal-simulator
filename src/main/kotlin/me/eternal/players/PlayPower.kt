package me.eternal.players

import me.eternal.engine.Card
import me.eternal.engine.GameLogProjection
import me.eternal.engine.PlayCardRequest
import me.eternal.engine.PlayerId
import me.eternal.engine.PlayerRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class PlayPower(val playerId: PlayerId) : PlayerEngine {
    override fun action(log: GameLogProjection): PlayerRequest? {
        val state = log.gameLog.state
        val me = state.player(playerId)
        if (log.upcomingActions.isNotEmpty()) {
            logger.debug("Waiting for ${log.upcomingActions} to be resolved.")
            return null
        }
        return if (me.powerPlayed) {
            logger.debug("Power already played.")
            null
        } else {
            val powerCard = me.hand.powers().firstOrNull()
            if (powerCard == null) {
                logger.info("No Power card.")
                null
            } else {
                logger.info("Playing ${powerCard.card} on turn ${log.gameLog.state.turn}.")
                PlayCardRequest(playerId, powerCard.idx)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PlayPower::class.simpleName)
    }
}

data class PlayCard(val playerId: PlayerId, val card: Card) : PlayerEngine {
    override fun action(log: GameLogProjection): PlayerRequest? {
        if (log.upcomingActions.isNotEmpty()) {
            PlayPower.logger.debug("Waiting for ${log.upcomingActions} to be resolved.")
            return null
        }

        val me = log.player(playerId)
        val cards = me.hand.select { it.card == card }
        val playCard = cards.firstOrNull()
        return if (playCard == null) {
            null
        } else {
            if (me.canPlay(playCard.idx))
                PlayCardRequest(playerId, playCard.idx)
            else
                null
        }
    }
}