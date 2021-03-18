package me.eternal.players

import me.eternal.engine.Decider
import me.eternal.engine.PlayerId
import me.eternal.engine.SeekPower
import me.eternal.players.ChainDecider
import me.eternal.players.DiscardNonPowerFirst
import me.eternal.players.Dummy
import me.eternal.players.HandCheck.Companion.atLeast
import me.eternal.players.HandCheck.Companion.power
import me.eternal.players.PlayCard
import me.eternal.players.PlayPower
import me.eternal.players.TurnLimiter

interface DeciderLibrary {
    fun get(player: String, playerId: PlayerId): Decider
}

object LocalPlayers : DeciderLibrary {
    override fun get(player: String, playerId: PlayerId): Decider = deciders(playerId).getValue(player)

    private fun deciders(playerId: PlayerId) = mapOf(
        "dummy" to Dummy(playerId),
        "play-power" to ChainDecider.builder(playerId) {
            muliganEngine = power(atLeast(2))
            add(TurnLimiter(playerId))
            add(PlayPower(playerId))
            add(PlayCard(playerId, SeekPower))
            special(DiscardNonPowerFirst(playerId))
        }.build()
    )
}