package com.perflyst.twire.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by Sebastian Rask Jepsen on 11-08-2015.
 */
@Parcelize
class Game(
    @JvmField
    val gameTitle: String,
    @JvmField
    val gameId: String,
    @JvmField
    var gameViewers: Int = -1,
    private var gameStreamers: Int = -1,
    @JvmField val previewTemplate: String?,
) : Comparable<Game>, Parcelable {
    constructor(game: com.github.twitch4j.helix.domain.Game) : this(
        game.name,
        game.id,
        -1,
        -1,
        game.boxArtUrl
    )

    override fun equals(other: Any?): Boolean = when (other) {
        is Game -> gameTitle == other.gameTitle
        else -> false
    }

    override fun hashCode(): Int = gameTitle.hashCode()

    override fun compareTo(other: Game): Int = this.gameViewers - other.gameViewers
}
