package com.perflyst.twire.model

import android.content.Context
import android.os.Parcelable
import com.perflyst.twire.R
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
    override val previewTemplate: String?,
) : Comparable<Game>, MainElement, Parcelable {
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

    override val width: String
        get() = "300"

    override val height: String
        get() = "400"

    override fun getPlaceHolder(context: Context): Int = R.drawable.template_game
}
