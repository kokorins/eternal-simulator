package me.eternal.players

import me.eternal.engine.Decider
import me.eternal.engine.Decision
import me.eternal.engine.GameLogProjection
import me.eternal.engine.PlayerId
import me.eternal.engine.PlayerRequest
import me.eternal.decks.Hand

class ChainDecider(val playerId: PlayerId, val muliganEngine: MuliganEngine, val playerEngines: List<PlayerEngine>,
                   val specials: List<SpecialEngine>) : Decider {

    override val me: PlayerId
        get() = playerId

    override fun muligan(order: Int, hand: Hand, muliganCount: Int) = muliganEngine.muligan(order, hand, muliganCount)

    override fun act(log: GameLogProjection): PlayerRequest? {
        return playerEngines.asSequence().mapNotNull { it.action(log) }.firstOrNull()
    }

    override fun decide(decision: Decision, log: GameLogProjection,
                        default: (PlayerId, GameLogProjection) -> List<PlayerRequest>): List<PlayerRequest> {
        return specials.asSequence().map { it.make(decision, log) }.filterNot { it.isEmpty() }.firstOrNull()
                ?: default(me, log)
    }

    class Builder(val playerId: PlayerId, val engines: MutableList<PlayerEngine> = mutableListOf(),
                  val specials: MutableList<SpecialEngine> = mutableListOf()) {
        lateinit var muliganEngine: MuliganEngine
        fun muligan(block: (Int, Hand, Int) -> Boolean) {
            muliganEngine = object : MuliganEngine {
                override fun muligan(order: Int, hand: Hand, muliganCount: Int): Boolean {
                    return block(order, hand, muliganCount)
                }
            }
        }

        fun add(engine: PlayerEngine) {
            engines.add(engine)
        }

        fun special(special: SpecialEngine) {
            specials.add(special)
        }

        fun build() = ChainDecider(playerId, muliganEngine, engines.toList(), specials.toList())
    }

    companion object {
        fun builder(playerId: PlayerId, block: Builder.() -> Unit): Builder {
            val builder = Builder(playerId)
            block(builder)
            return builder
        }

    }
}