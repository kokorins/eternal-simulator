package me.eternal.engine

sealed class GameEvent {
    data class PlayCard(val playerId: PlayerId, val idx: Int) : GameEvent()
    data class DrawCard(val playerId: PlayerId, val idx: Int) : GameEvent()
    data class EndTurn(val playerId: PlayerId) : GameEvent()
    data class EndGame(val playerId: PlayerId) : GameEvent()
    data class TurnStart(val playerId: PlayerId) : GameEvent()
    data class ReplenishPower(val playerId: PlayerId) : GameEvent()
    object NextTurn : GameEvent() {
        override fun toString(): String = this.javaClass.simpleName
    }

    data class Discard(val playerId: PlayerId, val idx: Int) : GameEvent()
}