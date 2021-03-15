package me.eternal.engine

import me.eternal.decks.FullLibrary
import me.eternal.decks.OrderedCard
import me.eternal.engine.Influence.Companion.colors
import me.eternal.engine.Influence.Companion.covers
import me.eternal.engine.Influence.Companion.summary

data class Field(val cards: MutableMap<Int, OrderedCard> = mutableMapOf())

data class PowerState(
    val power: Int = 0,
    val maxPower: Int = 0,
    val influences: Map<Influence, Int> = mapOf<Influence, Int>().withDefault { 0 }
) {
    fun summary(): String {
        return "$power/$maxPower(${influences.summary()})"
    }

    fun canAfford(requirements: Requirements): Boolean {
        return power >= requirements.power() && influences.covers(requirements.influences())
    }

    fun add(influences: Map<Influence, Int>) = this.copy(influences = colors().map { color ->
        color to (this.influences.getOrDefault(color, 0) + influences.getOrDefault(color, 0))
    }.toMap())

    fun expense(requirements: Requirements) = this.copy(power = this.power - requirements.power())
    fun replanish() = this.copy(power = maxPower)
    fun incrementMaxPower() = this.copy(maxPower = this.maxPower + 1)
    fun incrementPower(predicate: () -> Boolean) = if (predicate()) {
        this.copy(power = this.power + 1)
    } else {
        this
    }
}

data class Void(val cards: Map<Int, OrderedCard> = mapOf()) {
    fun extract(cardIdx: Int): Pair<Void, OrderedCard?> {
        val card = cards[cardIdx]
        return if (card != null) {
            Void(cards.minus(cardIdx)) to card
        } else {
            this to null
        }
    }

    fun add(card: OrderedCard) = Void(cards.plus(card.idx to card))
}

interface Rules {
    fun setup(players: Map<PlayerId, Player>): GameEngine = GameEngine(this, players)
    fun numMuligans(): Int
    fun initialHand(muliganCount: Int): Int
    fun initialDrawStrategy(muliganCount: Int): InitialDrawStrategy
    fun handThreshold(): Int
    fun checkGameEnd(log: GameLog): PlayerId?
    fun exceedAmount(log: GameLog): List<GameAction>
}

object Throne : Rules {
    private val cardLibrary = FullLibrary
    override fun numMuligans() = 3

    override fun initialHand(muliganCount: Int): Int {
        return if (muliganCount < 2) 7 else 6
    }

    override fun initialDrawStrategy(muliganCount: Int): InitialDrawStrategy {
        return if (muliganCount == 0) {
            Uniform(cardLibrary, initialHand(muliganCount))
        } else {
            TwoThreeFourPower(cardLibrary, initialHand(muliganCount))
        }
    }

//    override fun turnStarter(log: GameLog): List<GameAction> {
//        val playerId = log.nextPlayer()
//        val playersAction = listOf(TurnStart(playerId), DrawCard(playerId))
//        return if (log.night()) {
//            playersAction.plus(DrawCard(playerId)).reversed()
//        } else {
//            playersAction.reversed()
//        }
//    }

    override fun handThreshold(): Int = 11

    override fun checkGameEnd(log: GameLog): PlayerId? {
        return log.playersInOrder()
            .firstOrNull { (_, player) -> player.hasEmptyDeck() }
            ?.let { (playerId, _) -> playerId }
    }

    override fun exceedAmount(log: GameLog): List<GameAction> {
        return log.players().mapValues { (_, player) ->
            player.hand.cards.size - this.handThreshold()
        }.filterValues { it > 0 }.map { (playerId, n) -> Discard(playerId, n) }
    }
}