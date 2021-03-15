package me.eternal.engine

import org.slf4j.LoggerFactory

class Phase(val players: Map<PlayerId, Player>, val engine: GameEngine) {
    fun react(gameLog: GameLog, request: PlayerRequest): GameLog {
        return if (valid(request)) {
            consecutive(gameLog, listOf(request.toAction()))
        } else {
            logger.warn("Received invalid $request")
            gameLog
        }
    }

    fun consecutive(gameLog: GameLog, upcomingActions: List<GameAction>): GameLog {
        var log = gameLog
        var actions = upcomingActions
        while (actions.isNotEmpty()) {
            actions = simultaneous(gameLog, actions)
            val pair = engine.resolve(gameLog, actions)
            log = pair.first
            actions = pair.second
        }
        return log
    }

    private fun simultaneous(gameLog: GameLog, upcomingActions: List<GameAction>): List<GameAction> {
        var actions = upcomingActions
        val counterOrder = gameLog.setupLog.playersOrder(gameLog.state.currentPlayer())
        while (true) {
            val firstRequest = counterOrder.asSequence()
                .map { players.getValue(it) }
                .map { it.act(gameLog.projection(actions)) }
                .firstOrNull()
            if (firstRequest != null) {
                if (valid(firstRequest)) {
                    actions = listOf(firstRequest.toAction()).plus(actions)
                    continue
                } else {
                    logger.warn("Received invalid $firstRequest.")
                }
            } else {
                return actions
            }
        }
    }

    private fun valid(request: PlayerRequest): Boolean {
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Phase::class.simpleName)
    }
}