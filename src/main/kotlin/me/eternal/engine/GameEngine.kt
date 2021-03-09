package me.eternal.engine

import me.eternal.decks.Deck
import org.slf4j.LoggerFactory
import kotlin.random.Random

class GameEngine(private val rules: Rules, private val players: Map<PlayerId, Player>) {
    private val log = LoggerFactory.getLogger(this::class.simpleName)
    fun ignite(random: Random): GameLog {
        var gameLog = GameLog(initialize(random))
        log.info("Muligan finished: ${gameLog.setupLog.minimalSummary()}")
        do {
            gameLog = playTurn(gameLog, random)
            log.info("Turn finished: ${gameLog.state.minimalSummary()}")
        } while (!gameLog.state.end)
        return gameLog
    }

    private fun playTurn(gameLog: GameLog, random: Random): GameLog {
        var log = gameLog
        log = startPhase(log)
        log = actionPhase(log)
        log = endPhase(log)
        return log
    }

    private fun startPhase(gameLog: GameLog): GameLog {
        val startActions = rules.turnStarter(gameLog.state)
        val actions = Phase(players).play(gameLog, startActions)
        return resolve(gameLog, actions)
    }

    private fun actionPhase(gameLog: GameLog): GameLog {
        fun nonFinal(request: PlayerRequest): Boolean {
            return !(request is EndTurnRequest || request is EndGameRequest)
        }

        var log = gameLog
        val currentPlayerId = log.state.currentPlayer()
        val currentPlayer = players.getValue(currentPlayerId)
        var request = currentPlayer.act(GameLogProjection(log))
        while (request != null && nonFinal(request)) {
            val actions = Phase(players).play(log, listOf(request))
            log = resolve(log, actions)
            request = currentPlayer.act(GameLogProjection(log))
        }
        if (request is EndGameRequest) {
            return resolve(gameLog, listOf(request))
        }
        return log
    }

    private fun endPhase(gameLog: GameLog): GameLog {
        val endActions = rules.turnFinisher(gameLog, players)
        val actions = Phase(players).play(gameLog, endActions)
        return resolve(gameLog, actions)
    }

    private fun resolve(log: GameLog, actions: List<GameAction>): GameLog {
        var log = log
        for (action in actions.reversed()) {
            when (action) {
                is EndTurnRequest -> {
                    log = log.receive(GameEvent.EndTurn(action.playerId))
                }
                is EndGameRequest -> {
                    log = log.receive(GameEvent.EndGame(action.playerId))
                }
                is PlayCardRequest -> {
                    log = log.receive(GameEvent.PlayCard(action.playerId, action.cardIdx))
                }
                is DiscardRequest -> {
                    log = log.receive(GameEvent.Discard(action.playerId, action.cardIdx))
                }
                else -> {
                    log = log.receive(action.apply(log))
                }
            }
        }
        return log
    }

    private fun initialize(random: Random): SetupLog {
        val order = players.keys.toList().shuffled(random)
        val playerSetup = mutableMapOf<PlayerId, List<InitialDraw>>()
        for ((id, player) in players) {
            val draws = mutableListOf<InitialDraw>()

            for (muliganCount in 0 until rules.numMuligans()) {
                val draw = shuffleDeck(player.deck, rules, muliganCount, random)
                val accept = player.muligan(order.indexOf(id), draw.hand, muliganCount)
                draws.add(draw)
                if (accept)
                    break
            }
            playerSetup[id] = draws.toList()
        }
        return SetupLog(order, playerSetup)
    }

    private fun shuffleDeck(deck: Deck, rules: Rules, muliganCount: Int, random: Random): InitialDraw {
        val strategy = rules.initialDrawStrategy(muliganCount)
        return strategy.extract(deck, random)
    }
}