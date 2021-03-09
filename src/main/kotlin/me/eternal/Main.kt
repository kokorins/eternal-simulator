package me.eternal

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import me.eternal.engine.Throne
import org.slf4j.LoggerFactory

object Cli : CliktCommand() {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val players by option("-p", "--player", help = "Player logic").multiple(listOf("play-power"))
    private val decks by option("-d", "--deck", help = "Player's deck").multiple(listOf("empty-with-seek"))
    override fun run() {
        log.info("Running eternal engine with $players and $decks")
        val game = Game.of {
            seed = 0
            rules = Throne
            for (i in players.indices) {
                addPlayer(players[i], decks[i])
            }
        }
        val result = game.play()
        log.info(result.eventSummary())
    }
}

fun main(args: Array<String>) {
    Cli.main(args)
}