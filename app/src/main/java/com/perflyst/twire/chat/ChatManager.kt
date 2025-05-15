package com.perflyst.twire.chat

import android.os.SystemClock
import android.util.SparseArray
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.philippheuer.events4j.simple.domain.EventSubscriber
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.TwitchChatBuilder
import com.github.twitch4j.chat.events.ChatConnectionStateEvent
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.chat.events.channel.ClearChatEvent
import com.github.twitch4j.chat.events.channel.DeleteMessageEvent
import com.github.twitch4j.chat.events.channel.UserStateEvent
import com.github.twitch4j.chat.events.roomstate.ChannelStatesEvent
import com.github.twitch4j.client.websocket.domain.WebsocketConnectionState
import com.github.twitch4j.helix.domain.ChatBadgeSetList
import com.github.twitch4j.helix.domain.NamedUserChatColor
import com.google.common.collect.ImmutableSetMultimap
import com.netflix.hystrix.HystrixCommand
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.model.Badge
import com.perflyst.twire.model.ChatMessage
import com.perflyst.twire.model.Emote
import com.perflyst.twire.model.Emote.Companion.Twitch
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.chatAccountConnect
import com.perflyst.twire.service.Settings.chatEnableSSL
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.utils.Execute
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue

/*
* Created by SebastianRask on 03-03-2016.
*/

class ChatManager(aChannel: UserInfo, aVodId: String?, vodOffset: Int, aCallback: ChatCallback) :
    Runnable {
    private var currentProgress = -1.0
    private var cursor = ""
    private var seek = false
    private var previousProgress = 0.0
    private var account: OAuth2Credential? = null
    private val channel: UserInfo
    private val vodId: String?
    private val callback: ChatCallback
    private val mEmoteManager: ChatEmoteManager
    private var globalBadges: MutableMap<String, MutableMap<String, Badge>> =
        HashMap()
    private var channelBadges: MutableMap<String, MutableMap<String, Badge>> =
        HashMap()
    private val twitchChatServer: String

    private val vodLock = Any()
    private var nextCommentOffset = 0.0
    private var vodOffset = 0

    private var isStopping = false

    // Data about the user and how to display his/hers message
    var userDisplayName: String? = null
        private set
    var userColor: String? = null
        private set
    var userBadges: MutableMap<String, String>? = null
        private set

    private var twitchChat: TwitchChat? = null

    init {
        instance = this
        mEmoteManager = ChatEmoteManager(aChannel)

        Timber.d("Login with main Account: %s", chatAccountConnect)

        if (isLoggedIn && chatAccountConnect) { // if user is logged in ...
            // ... use their credentials
            Timber.d("Using user credentials for chat login.")

            account = TwireApplication.credential
        } else {
            // ... else: use anonymous credentials
            Timber.d("Using anonymous credentials for chat login.")

            account = null
        }

        channel = aChannel
        vodId = aVodId
        this.vodOffset = vodOffset
        callback = aCallback

        twitchChatServer =
            if (chatEnableSSL) TwitchChat.TWITCH_WEB_SOCKET_SERVER else "ws://irc-ws.chat.twitch.tv:80"
        Timber.d("Use SSL Chat Server: %s", chatEnableSSL)

        nextCommentOffset = 0.0
    }

    fun updateVodProgress(aCurrentProgress: Long, aSeek: Boolean) {
        currentProgress = (aCurrentProgress / 1000f + vodOffset).toDouble()
        seek = seek or aSeek

        // Only notify the thread when there's work to do.
        if (!aSeek && currentProgress < nextCommentOffset) return

        synchronized(vodLock) {
            (vodLock as Object).notify()
        }
    }

    fun setPreviousProgress() {
        previousProgress = currentProgress
        cursor = ""
    }

    override fun run() {
        isStopping = false
        Timber.d("Trying to start chat %s", channel.login)
        mEmoteManager.loadCustomEmotes {
            Execute.ui {
                callback.onCustomEmoteIdFetched(
                    mEmoteManager.channelCustomEmotes,
                    mEmoteManager.globalCustomEmotes
                )
            }
        }

        globalBadges = readBadges(TwireApplication.helix.getGlobalChatBadges(null))
        channelBadges =
            readBadges(TwireApplication.helix.getChannelChatBadges(null, channel.userId))
        readFFZBadges()

        if (vodId == null) {
            connect()
        } else {
            processVodChat()
        }
    }

    private fun onMessage(message: ChatMessage) {
        Execute.ui { callback.onMessage(message) }
    }

    private fun onState(state: WebsocketConnectionState) {
        Execute.ui { callback.onConnectionChanged(state) }
    }

    /**
     * Connect to twitch with the users twitch name and oauth key.
     * Joins the chat hashChannel.
     * Sends request to retrieve emote id and positions as well as username color
     * Handles parsing messages, pings and disconnects.
     * Inserts emotes, subscriber, turbo and mod drawables into messages. Also Colors the message username by the user specified color.
     * When a message has been parsed it is sent via the callback interface.
     */
    private fun connect() {
        twitchChat = TwitchChatBuilder.builder()
            .withChatAccount(account)
            .withBaseUrl(twitchChatServer)
            .build()

        twitchChat!!.joinChannel(channel.login)

        val eventManager = twitchChat!!.eventManager
        eventManager.getEventHandler(SimpleEventHandler::class.java)
            .registerListener(this)
    }

    private class VODComment(val contentOffset: Double, val data: JSONObject)

    private fun processVodChat() {
        try {
            synchronized(vodLock) {
                onState(WebsocketConnectionState.CONNECTED)
            }

            // Make sure that current progress has been set.
            synchronized(vodLock) {
                while (currentProgress == -1.0 && !isStopping) (vodLock as Object).wait()
            }

            val downloadedComments: Queue<VODComment> = LinkedList()
            var reconnecting = false
            var justSeeked = false
            while (!isStopping) {
                if (seek) {
                    seek = false
                    cursor = ""
                    downloadedComments.clear()
                    previousProgress = 0.0
                    justSeeked = true
                }

                var comment = downloadedComments.peek()
                if (comment == null) {
                    val dataObject = Service.graphQL(
                        "VideoCommentsByOffsetOrCursor",
                        "b70a3591ff0f4e0313d126c6a1502d79a1c02baebb288227c582044aa76adf6a",
                        object : HashMap<String, Any?>() {
                            init {
                                put("videoID", vodId)
                                if (cursor.isEmpty()) put(
                                    "contentOffsetSeconds",
                                    currentProgress.toInt()
                                )
                                else put("cursor", cursor)
                            }
                        })

                    if (dataObject == null) {
                        reconnecting = true
                        onState(WebsocketConnectionState.RECONNECTING)
                        SystemClock.sleep(2500)
                        continue
                    } else if (reconnecting) {
                        reconnecting = false
                        onState(WebsocketConnectionState.CONNECTED)
                    }

                    if (dataObject.getJSONObject("video").isNull("comments")) {
                        cursor = ""
                        continue
                    }

                    val commentsObject = dataObject.getJSONObject("video").getJSONObject("comments")
                    val comments = commentsObject.getJSONArray("edges")

                    for (i in 0..<comments.length()) {
                        val commentJSON = comments.getJSONObject(i).getJSONObject("node")
                        val contentOffset = commentJSON.getInt("contentOffsetSeconds")
                        // Don't show previous comments and don't show comments that came before the current progress unless we just seeked.
                        if (contentOffset < previousProgress || contentOffset < currentProgress && !justSeeked) continue

                        // Sometimes the commenter is null, Twitch doesn't show them so we won't either.
                        if (commentJSON.isNull("commenter")) continue

                        downloadedComments.add(VODComment(contentOffset.toDouble(), commentJSON))
                    }

                    justSeeked = false

                    val pageInfo = commentsObject.getJSONObject("pageInfo")
                    val hasNextPage = pageInfo.getBoolean("hasNextPage")
                    // Assumption: If the VOD has no comments and no previous or next comments, there are no comments on the VOD.
                    if (comments.length() == 0 && !hasNextPage && !pageInfo.getBoolean("hasPreviousPage")) {
                        break
                    }

                    if (hasNextPage) {
                        cursor = comments.getJSONObject(comments.length() - 1).getString("cursor")
                    } else if (downloadedComments.isEmpty()) {
                        // We've reached the end of the comments, nothing to do until the user seeks.
                        synchronized(vodLock) {
                            while (!seek && !isStopping) {
                                (vodLock as Object).wait()
                            }
                        }
                    }

                    comment = downloadedComments.peek()
                }

                if (seek || comment == null) {
                    continue
                }

                nextCommentOffset = comment.contentOffset
                synchronized(vodLock) {
                    while (currentProgress < nextCommentOffset && !seek && !isStopping) (vodLock as Object).wait()
                }

                // If the user seeked, don't display this comment since it would now be an old comment.
                if (seek) continue

                val commenter = comment.data.getJSONObject("commenter")
                val message = comment.data.getJSONObject("message")

                val badges: MutableMap<String, String> = HashMap()
                if (message.has("userBadges")) {
                    val userBadgesArray = message.getJSONArray("userBadges")
                    for (j in 0..<userBadgesArray.length()) {
                        val userBadge = userBadgesArray.getJSONObject(j)
                        val setID = userBadge.getString("setID")
                        val version = userBadge.getString("version")
                        if (setID.isEmpty() || version.isEmpty()) continue

                        badges.put(setID, version)
                    }
                }

                val color =
                    if (!message.isNull("userColor")) message.getString("userColor") else null
                val displayName = commenter.getString("displayName")

                val bodyBuilder = StringBuilder()
                val fragments = message.getJSONArray("fragments")
                // Some messages have no fragments, no idea why. Twitch skips them so we will too.
                if (fragments.length() == 0) {
                    downloadedComments.poll()
                    continue
                }

                val emotes: MutableMap<Int, Emote> = HashMap()
                for (i in 0..<fragments.length()) {
                    val fragment = fragments.getJSONObject(i)
                    val text = fragment.getString("text")

                    val emote = fragment.optJSONObject("emote")
                    if (emote != null) {
                        emotes.put(bodyBuilder.length, Twitch(text, emote.getString("emoteID")))
                    }

                    bodyBuilder.append(text)
                }

                val body = bodyBuilder.toString()
                emotes.putAll(mEmoteManager.findCustomEmotes(body))

                //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();
                val chatMessage =
                    ChatMessage(body, displayName, color, getBadges(badges), emotes, false)
                onMessage(chatMessage)

                downloadedComments.poll()
            }
        } catch (e: Exception) {
            Timber.e(e)

            onState(WebsocketConnectionState.LOST)
            SystemClock.sleep(2500)
            processVodChat()
        }

        currentProgress = -1.0
    }

    @EventSubscriber
    private fun handleSocketState(state: ChatConnectionStateEvent) {
        onState(state.state)
    }

    /**
     * Handles the room state by notifying the chatfragment
     */
    @EventSubscriber
    private fun handleRoomState(event: ChannelStatesEvent) {
        Execute.ui { callback.onRoomStateChange(event) }
    }

    /**
     * Handles the event and saves data such as the users color, display name, and emotes
     */
    @EventSubscriber
    private fun handleUserState(event: UserStateEvent) {
        if (userDisplayName != null) return

        userBadges = event.messageEvent.getBadges()
        userColor = event.color.orElse("")
        userDisplayName = event.displayName.orElse("")
        callback.onEmoteSetsFetched(event.emoteSets)
    }

    /**
     * Parses and builds retrieved messages.
     * Sends build message back via callback.
     */
    @EventSubscriber
    private fun handleMessage(message: ChannelMessageEvent) {
        val messageEvent = message.messageEvent
        val badges = messageEvent.getBadges()
        val displayName = message.user.name
        val color = messageEvent.getTagValue("color").orElse(randomColor(displayName))
        val content = message.message
        val emotes =
            mEmoteManager.findTwitchEmotes(messageEvent.getTagValue("emotes").orElse(""), content)
        emotes.putAll(mEmoteManager.findCustomEmotes(content))

        //Pattern.compile(Pattern.quote(userDisplayName), Pattern.CASE_INSENSITIVE).matcher(message).find();
        val chatMessage = ChatMessage(content, displayName, color, getBadges(badges), emotes, false)
        chatMessage.id = message.eventId
        chatMessage.systemMessage = messageEvent.getTagValue("system-msg").orElse("")

        if (content.contains("@${this.userDisplayName}")) {
            Timber.d("Highlighting message with mention: %s", content)
            chatMessage.isHighlight = true
        }

        onMessage(chatMessage)
    }

    private fun randomColor(username: String): String? {
        val colors: Array<NamedUserChatColor?> = NamedUserChatColor.entries.toTypedArray()
        return colors[username.hashCode() % colors.size]!!.hexCode
    }

    @EventSubscriber
    private fun handleClearChat(event: ClearChatEvent?) {
        callback.onClear(null)
    }

    @EventSubscriber
    private fun handleClearMessage(event: DeleteMessageEvent) {
        callback.onClear(event.msgId)
    }

    /**
     * Makes the ChatManager stop retrieving messages.
     */
    fun stop() {
        isStopping = true

        if (twitchChat != null) twitchChat!!.close()

        synchronized(vodLock) {
            (vodLock as Object).notify()
        }
    }

    /**
     * Send a message to a hashChannel on Twitch (Don't need to be on that hashChannel)
     *
     * @param message The message that will be sent
     */
    fun sendMessage(message: String?) {
        twitchChat!!.sendMessage(channel.login, message)
    }

    private fun readBadges(request: HystrixCommand<ChatBadgeSetList>): MutableMap<String, MutableMap<String, Badge>> {
        return object : HashMap<String, MutableMap<String, Badge>>() {
            init {
                for (badgeSet in request.execute()!!.badgeSets) {
                    put(badgeSet.setId, object : HashMap<String, Badge>() {
                        init {
                            for (badge in badgeSet.versions) {
                                put(
                                    badge.id, Badge(
                                        badgeSet.setId,
                                        object : SparseArray<String>() {
                                            init {
                                                put(1, badge.smallImageUrl)
                                                put(2, badge.mediumImageUrl)
                                                put(4, badge.largeImageUrl)
                                            }
                                        })
                                )
                            }
                        }
                    })
                }
            }
        }
    }

    private fun readFFZBadges() {
        val mapBuilder = ImmutableSetMultimap.builder<String?, Badge?>()

        try {
            val topObject =
                JSONObject(Service.urlToJSONString("https://api.frankerfacez.com/v1/badges"))
            val badges = topObject.getJSONArray("badges")
            val users = topObject.getJSONObject("users")
            for (badgeIndex in 0..<badges.length()) {
                val badgeJSON = badges.getJSONObject(badgeIndex)

                val urls = SparseArray<String>()
                val urlsObject = badgeJSON.getJSONObject("urls")
                val iterator = urlsObject.keys()
                while (iterator.hasNext()) {
                    val size = iterator.next()
                    urls.put(size.toInt(), urlsObject.getString(size))
                }

                val badge = Badge(
                    badgeJSON.getString("name"),
                    urls,
                    badgeJSON.getString("color"),
                    if (badgeJSON.isNull("replaces")) null else badgeJSON.getString("replaces")
                )
                val badgeUsers = users.getJSONArray(badgeJSON.getString("id"))
                for (userIndex in 0..<badgeUsers.length()) {
                    mapBuilder.put(badgeUsers.getString(userIndex), badge)
                }
            }

            ffzBadgeMap = mapBuilder.build()
        } catch (e: JSONException) {
            Timber.e(e)
        }
    }

    private fun getBadge(badgeSet: String, version: String): Badge? {
        val channelSet = channelBadges[badgeSet]
        if (channelSet != null && channelSet[version] != null) return channelSet[version]

        val globalSet = globalBadges[badgeSet]
        if (globalSet != null && globalSet[version] != null) return globalSet[version]

        Timber.e("Badge failed to load: \"$badgeSet\" \"$version\"")
        return null
    }

    fun getBadges(badges: MutableMap<String, String>): MutableList<Badge?> {
        val badgeObjects: MutableList<Badge?> = ArrayList()
        for (entry in badges.entries) {
            badgeObjects.add(getBadge(entry.key, entry.value))
        }

        return badgeObjects
    }

    interface ChatCallback {
        fun onMessage(message: ChatMessage)

        fun onClear(target: String?)

        fun onConnectionChanged(state: WebsocketConnectionState)

        fun onRoomStateChange(state: ChannelStatesEvent)

        fun onCustomEmoteIdFetched(channel: MutableList<Emote>, global: MutableList<Emote>)

        fun onEmoteSetsFetched(emoteSets: MutableList<String>?)
    }

    companion object {
        var instance: ChatManager? = null

        var ffzBadgeMap: ImmutableSetMultimap<String, Badge>? = null
    }
}

