package me.eternal.stats

import com.google.common.math.Quantiles
import me.eternal.engine.GameEvent
import me.eternal.engine.GameLog
import me.eternal.engine.GameState
import me.eternal.engine.PlayerId
import me.eternal.stats.Summary.Companion.whenever

interface Summary {
    fun analyze(log: GameLog)
    fun summarize(): String

    data class Calculation<T>(val condition: (GameState, GameEvent) -> Boolean, val calculation: (GameState) -> T)

    class Builder<T>(private val condition: (GameState, GameEvent) -> Boolean) {
        fun calculate(calculation: (GameState) -> T): Calculation<T> = Calculation(condition, calculation)
    }

    companion object {
        fun <T> whenever(condition: (GameState, GameEvent) -> Boolean): Builder<T> {
            return Builder(condition)
        }
    }
}


class PowerProfile(val playerId: PlayerId) : Summary {
    val activePower = whenever<Int> { state, event -> state.turn == 8 && event is GameEvent.TurnFinished }
        .calculate { state ->
            state.player(playerId).power.power
        }

    val sequence = mutableListOf<Int>()
    override fun analyze(log: GameLog) {
        var state = GameState.init(log.setupLog)
        for (event in log.events) {
            if (activePower.condition(state, event)) {
                sequence.add(activePower.calculation(state))
            }
            state = state.receive(event)
        }
    }

    override fun summarize(): String {
        return "Median on turn 5 - ${Quantiles.median().compute(sequence)}"
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
        return "power-profile" to PowerProfile(players.first())
    }


    private val summaries = mapOf(powerProfile())
}