package me.eternal.engine

/**
 * Intention that should happen
 */
interface GameAction

data class EndGame(val playerId: PlayerId) : GameAction

object TurnStart : GameAction

data class Discard(val playerId: PlayerId, val numCards: Int) : GameAction

data class DiscardCard(val playerId: PlayerId, val idx: Int) : GameAction

object TurnEnd : GameAction

data class DrawCard(val playerId: PlayerId) : GameAction

data class TutorCard(val playerId: PlayerId, val cardIdx: Int) : GameAction

data class PlayCard(val playerId: PlayerId, val cardIdx: Int) : GameAction