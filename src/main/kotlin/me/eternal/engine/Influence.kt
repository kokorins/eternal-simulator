package me.eternal.engine

enum class Influence(val symbol: String) {
    Fire("F"),
    Time("T"),
    Justice("J"),
    Shadow("S"),
    Primal("P"),
    Gray("G");

    companion object {
        fun colors() = listOf(Fire, Time, Justice, Shadow, Primal)
        fun Map<Influence, Int>.covers(that: Map<Influence, Int>): Boolean {
            return colors().all { color -> this.getValue(color) >= that.getValue(color) }
        }

        fun Map<Influence, Int>.summary(): String {
            return this.filterValues { it > 0 }.mapKeys { it.key.symbol }.entries.joinToString { it.toString() }
        }
    }
}