package me.eternal.stats

import com.google.common.collect.ArrayListMultimap
import com.google.common.math.Quantiles
import me.eternal.engine.GameEvent
import me.eternal.engine.GameLog
import me.eternal.engine.GameState
import me.eternal.engine.PlayerId
import me.eternal.stats.Summary.Companion.before

interface Summary {
    fun analyze(log: GameLog)
    fun summarize(): String

    data class Calculation<T>(
        val before: Boolean,
        val condition: (GameState, GameEvent) -> Boolean,
        val metric: (GameState) -> T,
        val aggreagation: (List<T>) -> String
    )

    class Builder<T>(val before: Boolean, private val condition: (GameState, GameEvent) -> Boolean) {
        fun calculate(calculation: (GameState) -> T) = Builder2(before, condition, calculation)
    }

    class Builder2<T>(
        val before: Boolean,
        private val condition: (GameState, GameEvent) -> Boolean,
        private val calculation: (GameState) -> T
    ) {
        fun aggregate(aggragation: (List<T>) -> String): Calculation<T> =
            Calculation(before, condition, calculation, aggragation)
    }

    companion object {
        fun <T> before(condition: (GameState, GameEvent) -> Boolean): Builder<T> {
            return Builder(true, condition)
        }

        fun <T> after(condition: (GameState, GameEvent) -> Boolean): Builder<T> {
            return Builder(false, condition)
        }
    }

    class KeyedSequence<T, V>(val name: String, private val calculation: Calculation<Pair<T, V>>) {
        val sequence = ArrayListMultimap.create<T, V>()
        fun before(state: GameState, event: GameEvent) {
            if (calculation.before && calculation.condition(state, event)) {
                val calculation = calculation.metric(state)
                sequence.put(calculation.first, calculation.second)
            }
        }

        fun after(state: GameState, event: GameEvent) {
            if (!calculation.before && calculation.condition(state, event)) {
                val calculation = calculation.metric(state)
                sequence.put(calculation.first, calculation.second)
            }
        }

        fun summarize() = calculation.aggreagation(sequence.entries().map { pair -> pair.toPair() })
    }
}

class CombinedSummary<T>(val name: String, val playerId: PlayerId, val summaries: List<Summary.KeyedSequence<T, *>>) :
    Summary {
    override fun analyze(log: GameLog) {
        var state = GameState.init(log.setupLog)
        for (event in log.events) {
            summaries.forEach { it.before(state, event) }
            state = state.receive(event)
            summaries.forEach { it.after(state, event) }
        }
    }

    override fun summarize(): String {
        val builder = StringBuilder()
        builder.appendLine(name)
        for (summary in summaries) {
            builder.appendLine(summary.name)
            builder.append(summary.summarize())
        }
        return builder.toString()
    }

    companion object {
        fun powerProfile(playerId: PlayerId) = CombinedSummary("power-profile", playerId, listOf(
            Summary.KeyedSequence(
                "active-power",
                before<Pair<Int, Int>> { _, event -> event is GameEvent.TurnFinished }
                    .calculate { state ->
                        state.turn to state.player(playerId).power.power
                    }.aggregate {
                        val turns = it.groupBy({ pair -> pair.first }, {pair->pair.second})
                        val builder = StringBuilder()
                        for ((turn, power) in turns) {
                            builder.appendLine("Median on turn $turn: ${Quantiles.median().compute(power)}")
                        }
                        builder.toString()
                    }),
            Summary.KeyedSequence(
                "max-power",
                before<Pair<Int, Int>> { _, event -> event is GameEvent.TurnFinished }
                    .calculate { state ->
                        state.turn to state.player(playerId).power.maxPower
                    }.aggregate {
                        val turns = it.groupBy({ pair -> pair.first }, {pair->pair.second})
                        val builder = StringBuilder()
                        for ((turn, power) in turns) {
                            builder.appendLine("Median on turn $turn: ${Quantiles.median().compute(power)}")
                        }
                        builder.toString()
                    })
        )
        )
    }
}

class LocalSummaries(val players: List<PlayerId>) {
    fun get(vararg names: String): Summary {
        return if (names.size == 1) {
            summaries.getValue(names.first())
        } else {
            TODO()
        }
    }

    fun powerProfile(): Pair<String, Summary> {
        return "power-profile" to CombinedSummary.powerProfile(players.first())
    }


    private val summaries = mapOf(powerProfile())
}