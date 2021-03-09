package me.eternal.engine

import me.eternal.decks.CardId

data class Requirements(val requirementMap: Map<Influence, Int> = mapOf<Influence, Int>().withDefault { 0 }) {
    fun power() = requirementMap.getValue(Influence.Gray)
    fun influences(): Map<Influence, Int> {
        return Influence.colors().map { it to requirementMap.getValue(it) }.toMap()
    }

    data class Builder(val requirementMap: MutableMap<Influence, Int> = mutableMapOf()) {
        fun require(influence: Influence, n: Int): Builder {
            requirementMap[influence] = n
            return this
        }

        fun power(n: Int): Builder = require(Influence.Gray, n)
        fun colors(fire: Int = 0, time: Int = 0, justice: Int = 0, shadow: Int = 0, primal: Int = 0): Builder {
            requirementMap[Influence.Fire] = fire
            requirementMap[Influence.Time] = time
            requirementMap[Influence.Justice] = justice
            requirementMap[Influence.Shadow] = shadow
            requirementMap[Influence.Primal] = primal
            return this
        }
    }

    companion object {
        fun of(block: Builder.() -> Unit): Requirements {
            val builder = Builder()
            block(builder)
            return Requirements(builder.requirementMap.toMap().withDefault { 0 })
        }
    }
}

interface Power : Card {
    fun depleted(): Boolean
    fun influences(): Map<Influence, Int>
}

interface Summon : Card {
    fun act(): List<Decision>
}

data class Sigil(val influence: Influence) : Power {
    override val id: CardId = CardId("Sigil $influence")
    override fun isPower(): Power? = this
    override fun requirements() = Requirements()
    override fun depleted() = false

    override fun influences(): Map<Influence, Int> = mapOf(influence to 1).withDefault { 0 }
}

object JustPower : Power {
    override val id = CardId("just-power")
    override fun isPower(): Power? = this
    override fun requirements() = Requirements()
    override fun depleted() = false

    override fun influences() = mapOf(Influence.Gray to 1).withDefault { 0 }
    override fun toString(): String = this.javaClass.simpleName
}

data class JustCard(val cost: Int) : Card {
    override val id = CardId("just-card")
    override fun isPower(): Power? = null
    override fun requirements() = Requirements(mapOf(Influence.Gray to cost).withDefault { 0 })
}

object SeekPower : Card, Summon {
    override fun act(): List<Decision> {
        return listOf(DrawSigilDecision)
    }

    override fun toString() = "SeekPower"

    override val id: CardId = CardId("#408")

    override fun isPower(): Power? = null
    override fun isSummon(): Summon? = this

    override fun requirements(): Requirements = Requirements.of { power(1) }
}

interface Card {
    val id: CardId
    fun isPower(): Power? = null
    fun isSummon(): Summon? = null
    fun requirements(): Requirements
    fun power() = isPower() != null
}
