package me.eternal

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import me.eternal.engine.PlayerId
import me.eternal.engine.Throne
import me.eternal.stats.LocalSummaries
import org.slf4j.LoggerFactory
import kotlin.random.Random

object Cli : CliktCommand() {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val numGames by option("-n", "--num-games", help = "Number of games").default("100")
    private val summary by option("-s", "--summary", help = "Summaries").multiple(listOf("power-profile"))
    private val decider by option("-p", "--player", help = "Player logic").multiple(listOf("play-power"))
    private val decks by option("-d", "--deck", help = "Player's deck").multiple(listOf("empty-with-seek"))
    private val seed by option("--seed", help = "Initial seed").default("0")
    override fun run() {
        val random = Random(seed.toInt())
        val playerIds = decider.indices.map { PlayerId(it) }
        val tracker = LocalSummaries(playerIds).get(*summary.toTypedArray())
        for (i in 1..numGames.toInt()) {
            val seed = random.nextInt()
            log.info("Running game $i with $decider and $decks and seed $seed.")
            val game = Game.of {
                this.seed = seed
                rules = Throne
                for (idx in decider.indices) {
                    addPlayer(decider[idx], decks[idx])
                }
            }
            val result = game.play()
            tracker.analyze(result)
        }
        log.info(tracker.summarize())
    }
}

fun main(args: Array<String>) {
    Cli.main(args)
}