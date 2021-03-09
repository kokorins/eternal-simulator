package me.eternal

import me.eternal.engine.*
import kotlin.random.Random

class Game(private val players: Map<PlayerId, Player>, private val rules: Rules, private val seed: Int) {

    fun play(): GameLog {
        val random = Random(seed)
        val engine = rules.setup(players)
        return engine.ignite(random)
    }

    class Builder(private val playerProvider: PlayerProvider) {
        private val players = mutableListOf<Pair<String, String>>()
        var seed = 0
        lateinit var rules: Rules

        fun addPlayer(playerId: String, deck: String) {
            players.add(playerId to deck)
        }

        fun build(): Game {
            return Game(players.mapIndexed { i, (p, d) -> PlayerId(i) to playerProvider.create(PlayerId(i), p, d) }.toMap(), rules, seed)
        }
    }

    companion object {
        fun builder(block: Builder.() -> Unit): Builder {
            val builder = Builder(DefaultPlayerProvider)
            block(builder)
            return builder
        }

        fun of(fn: Builder.() -> Unit): Game {
            return builder(fn).build()
        }
    }
}