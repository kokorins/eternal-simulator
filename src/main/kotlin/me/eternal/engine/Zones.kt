package me.eternal.engine

sealed class Zone {
    object Deck : Zone()
    object Hand : Zone()
    object Void : Zone()
}

class ZoneChange(private val playerId: PlayerId, val cardIdx: Int) {
    lateinit var from: Zone
    lateinit var to: Zone
    fun from(from: Zone): ZoneChange {
        this.from = from
        return this
    }

    fun to(to: Zone): ZoneChange {
        this.to = to
        return this
    }

    fun at(state: GameState): GameState {
        val playerState = state.player(playerId)
        return state.with(playerId, playerState.change(this))
    }
}