package me.eternal.engine

import org.slf4j.LoggerFactory

class Phase(val players: Map<PlayerId, Player>) {
    fun play(gameLog: GameLog, setup: List<GameAction>): List<GameAction> {
        val actions = setup.toMutableList()
        val counterOrder = gameLog.setupLog.orderStarting(gameLog.state.currentPlayer())
        while (true) {
            val action = counterOrder.asSequence().map { it to players.getValue(it) }
                    .map { (pId, p) ->
                        p.act(gameLog.projection(pId, actions.toList()))
                    }.firstOrNull()
            if (action != null) {
                if (valid(action)) {
                    actions.add(action)
                    continue
                } else {
                    logger.warn("Received invalid $action.")
                }
            }
            else {
                return actions
            }
        }
    }

    private fun valid(request: PlayerRequest): Boolean {
        return request is DiscardRequest
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Phase::class.simpleName)
    }
}