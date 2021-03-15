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
        val (log, actions) = resolve(gameLog, listOf(TurnStart))
        return Phase(players, this).consecutive(log, actions)
    }

    private fun actionPhase(gameLog: GameLog): GameLog {
        fun final(request: PlayerRequest): Boolean {
            return request is EndGameRequest
        }

        var log = gameLog
        val currentPlayerId = log.state.currentPlayer()
        val currentPlayer = players.getValue(currentPlayerId)
        var request = currentPlayer.act(GameLogProjection(log))
        while (request != null) {
            log = Phase(players, this).react(log, request)
            if (final(request)) {
                return log
            } else {
                request = currentPlayer.act(GameLogProjection(log))
            }
        }
        return log
    }

    private fun endPhase(gameLog: GameLog): GameLog {
        return Phase(players, this).consecutive(gameLog, listOf(TurnEnd))
    }

    fun resolve(gameLog: GameLog, simultaneousActions: List<GameAction>): Pair<GameLog, List<GameAction>> {
        if (simultaneousActions.isEmpty()) {
            return gameLog to simultaneousActions
        } else {
            val action = simultaneousActions.first()
            val tail = simultaneousActions.drop(1)
            when (action) {
                is EndGame -> {
                    return gameLog.receive(GameEvent.GameFinished(action.playerId)) to listOf()
                }
                is DiscardCard -> {
                    return resolve(gameLog.receive(GameEvent.CardDiscarded(action.playerId, action.idx)), tail)
                }
                is Discard -> {
                    val requests = DiscardDecision(action.playerId, action.numCards).make(
                        gameLog.projection(simultaneousActions),
                        players
                    )
                    return resolve(
                        requests.foldRight(gameLog) { request, log -> Phase(players, this).react(log, request) },
                        tail
                    )
                }
                is DrawCard -> {
                    val player = gameLog.player(action.playerId)
                    val cardIdx = player.deck.top()
                    return if (cardIdx != null) {
                        resolve(gameLog.receive(GameEvent.CardDrawn(action.playerId, cardIdx)), tail)
                    } else {
                        resolve(gameLog, tail)
                    }
                }
                is TurnEnd -> {
                    val looser = rules.checkGameEnd(gameLog)
                    return if (looser != null) {
                        gameLog.receive(GameEvent.GameFinished(looser)) to listOf()
                    } else {
                        val discards = rules.exceedAmount(gameLog)
                        if (discards.isNotEmpty()) {
                            resolve(gameLog, discards.plus(simultaneousActions))
                        } else {
                            resolve(gameLog.receive(GameEvent.TurnFinished(gameLog.currentPlayer())), tail)
                        }
                    }
                }
                is TutorCard -> {
                    return resolve(gameLog.receive(GameEvent.CardDrawn(action.playerId, action.cardIdx)), tail)
                }
                is PlayCard -> {
                    val card = gameLog.card(action.playerId, action.cardIdx)
                    return if (card != null) {
                        val power = card.isPower()
                        val powerEvents = if (power != null) {
                            val depleted = power.depleted(gameLog)
                            listOf(GameEvent.AddPower(action.playerId, depleted, power.influences()))
                        } else {
                            listOf()
                        }
                        val summon = card.isSummon()
                        val requests = if (summon != null) {
                            val decisions = summon.act(action.playerId)
                            decisions.flatMap { it.make(gameLog.projection(simultaneousActions), players) }
                        } else {
                            listOf()
                        }
                        val initial = gameLog.receive(
                            GameEvent.CardPlayed(action.playerId, action.cardIdx),
                            *(powerEvents.toTypedArray())
                        )
                        return resolve(
                            requests.foldRight(initial) { request, log -> Phase(players, this).react(log, request) },
                            tail
                        )
                    } else {
                        gameLog to tail
                    }
                }
                is TurnStart -> {
                    val looser = rules.checkGameEnd(gameLog)
                    return if (looser != null) {
                        resolve(gameLog.receive(GameEvent.GameFinished(looser)), listOf())
                    } else {
                        val drawActions = if (gameLog.night()) {
                            listOf(DrawCard(gameLog.currentPlayer()), DrawCard(gameLog.currentPlayer()))
                        } else {
                            listOf(DrawCard(gameLog.currentPlayer()))
                        }
                        resolve(
                            gameLog.receive(
                                GameEvent.TurnStarted(gameLog.currentPlayer()),
                                GameEvent.PowerReplenished(gameLog.currentPlayer())
                            ),
                            drawActions.plus(tail)
                        )
                    }
                }
                else -> {
                    throw Error("Unexpected $action")
                }
            }
        }
    }

//    private fun playCard(event: GameEvent.CardPlayed): GameState {
//        val player = player(event.playerId)
//        val (newHand, card) = player.hand.extract(event.idx)
//        val power = card?.isPower()
//        val newState = when {
//            power != null -> {
//                playPower(event.playerId, player.with(newHand), power)
//            }
//            card != null -> {
//                this.with(event.playerId, player.with(newHand).with(player.power.expense(card.requirements())))
//            }
//            else -> {
//                this
//            }
//        }
//        val summon = card?.isSummon()
//        if (summon != null) {
//            val decisions = summon.act(event.playerId)
//            for(decision in decisions) {
//                TODO()
//            }
//        }
//        return newState
//    }

//    private fun playPower(playerId: PlayerId, player: PlayerState, power: Power): GameState {
//        val builder = PlayerState.Builder(player)
//        builder.powerPlayed = true
//        builder.power = player.power.add(power.influences()).incrementMaxPower().incrementPower { !power.depleted() }
//        val newPlayerState = builder.build()
//        return this.with(playerId, newPlayerState)
//    }


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