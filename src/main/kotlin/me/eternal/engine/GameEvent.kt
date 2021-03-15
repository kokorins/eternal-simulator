package me.eternal.engine

sealed class GameEvent {
    data class CardPlayed(val playerId: PlayerId, val cardIdx: Int) : GameEvent()
    data class CardDrawn(val playerId: PlayerId, val cardIdx: Int) : GameEvent()
    data class TurnFinished(val playerId: PlayerId) : GameEvent()
    data class GameFinished(val playerId: PlayerId) : GameEvent()
    data class TurnStarted(val playerId: PlayerId) : GameEvent()
    data class PowerReplenished(val playerId: PlayerId) : GameEvent()
    data class CardDiscarded(val playerId: PlayerId, val cardIdx: Int) : GameEvent()
    data class AddPower(val playerId: PlayerId, val depleted: Boolean, val influences: Map<Influence, Int>) : GameEvent()
}