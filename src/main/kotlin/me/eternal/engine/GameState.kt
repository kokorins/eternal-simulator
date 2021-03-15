package me.eternal.engine

import me.eternal.decks.Hand
import me.eternal.decks.OrderedDeck

data class GameState(
    val setupLog: SetupLog, var turn: Int, var end: Boolean, val night: Boolean = false,
    val players: Map<PlayerId, PlayerState> = setupLog.playerSetups.mapValues {
        val draw = it.value.last()
        PlayerState(
            Hand(draw.hand.cards.toMutableMap()),
            Field(),
            PowerState(),
            Void(),
            draw.deck.copy(cards = draw.deck.cards.toMutableList())
        )
    }
) {

    fun with(playerId: PlayerId, playerState: PlayerState) =
        this.copy(players = this.players.plus(playerId to playerState))

    fun currentPlayer(): PlayerId = setupLog.order[turn % setupLog.order.size]
    fun player(playerId: PlayerId) = players.getValue(playerId)
    private fun move(playerId: PlayerId, cardIdx: Int) = ZoneChange(playerId, cardIdx)

    fun receive(event: GameEvent): GameState {
        return when (event) {
            is GameEvent.CardDrawn -> {
                move(event.playerId, event.cardIdx).from(Zone.Deck).to(Zone.Hand).at(this)
            }
            is GameEvent.TurnFinished -> {
                this.copy(turn = this.turn + 1)
            }
            is GameEvent.GameFinished -> {
                this.copy(end = true)
            }
            is GameEvent.TurnStarted -> {
                val player = player(event.playerId)
                this.with(event.playerId, player.copy(powerPlayed = false))
            }
            is GameEvent.PowerReplenished -> {
                val player = player(event.playerId)
                this.with(event.playerId, player.copy(power = player.power.replanish()))
            }
            is GameEvent.CardPlayed -> {
                //TODO: In case of unit move to Table
                val player = player(event.playerId)
                val card = player.hand.get(event.cardIdx)
                if (card != null) {
                    move(event.playerId, event.cardIdx).from(Zone.Hand).to(Zone.Void)
                        .at(with(event.playerId, player.with(player.power.expense(card.requirements()))))
                } else {
                    this
                }
            }
            is GameEvent.AddPower -> {
                val player = player(event.playerId)
                val builder = PlayerState.Builder(player)
                builder.powerPlayed = true
                builder.power =
                    player.power.add(event.influences).incrementMaxPower().incrementPower { !event.depleted }
                val newPlayerState = builder.build()
                return this.with(event.playerId, newPlayerState)

            }
            is GameEvent.CardDiscarded -> {
                move(event.playerId, event.cardIdx).from(Zone.Hand).to(Zone.Void).at(this)
            }
        }
    }

    fun minimalSummary(): String {
        return "$turn ${reportNight()} ${setupLog.minimalSummary()} ${playersMinimalSummary()}"
    }

    private fun reportNight() = if (night) "night" else ""

    private fun playersMinimalSummary(): String {
        return players.map { (playerId, state) -> "$playerId: ${state.minimalSummary()}" }.joinToString { it }
    }
}

data class PlayerState(
    val hand: Hand,
    val field: Field,
    val power: PowerState,
    val void: Void,
    val deck: OrderedDeck,
    val powerPlayed: Boolean = false
) {
    fun with(hand: Hand) = this.copy(hand = hand)
    fun with(power: PowerState) = this.copy(power = power)
    fun change(zoneChange: ZoneChange): PlayerState {
        val builder = Builder(this)
        val card = when (zoneChange.from) {
            is Zone.Deck -> {
                val (newDeck, card) = deck.extract(zoneChange.cardIdx)
                builder.deck = newDeck
                card
            }
            is Zone.Hand -> {
                val (newHand, card) = hand.extract(zoneChange.cardIdx)
                builder.hand = newHand
                card
            }
            is Zone.Void -> {
                val (newVoid, card) = void.extract(zoneChange.cardIdx)
                builder.void = newVoid
                card
            }
        }
        return if (card == null) {
            this
        } else {
            when (zoneChange.to) {
                is Zone.Hand -> builder.hand = builder.hand.add(card)
                is Zone.Void -> builder.void = builder.void.add(card)
                is Zone.Deck -> throw RuntimeException("Unexpected $zoneChange")
            }
            builder.build()
        }
    }

    fun minimalSummary(): String {
        return "p=${power.summary()}, #h=${hand.cards.size} #d=${deck.cards.size} #v=${void.cards.size}"
    }

    fun canPlay(cardIdx: Int): Boolean {
        return if (!hand.cards.containsKey(cardIdx))
            false
        else {
            val card = hand.cards.getValue(cardIdx)
            val powerCard = card.isPower()
            if (powerCard != null) {
                !powerPlayed
            } else {
                val requirements = card.requirements()
                this.power.canAfford(requirements)
            }
        }
    }

    fun hasEmptyDeck() = deck.cards.isEmpty()

    class Builder(
        var hand: Hand,
        var field: Field,
        var power: PowerState,
        var void: Void,
        var deck: OrderedDeck,
        var powerPlayed: Boolean = false
    ) {
        constructor(state: PlayerState) : this(
            state.hand,
            state.field,
            state.power,
            state.void,
            state.deck,
            state.powerPlayed
        )

        fun build() = PlayerState(hand, field, power, void, deck, powerPlayed)
    }
}