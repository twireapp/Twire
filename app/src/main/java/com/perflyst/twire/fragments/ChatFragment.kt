package com.perflyst.twire.fragments

import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.transition.Transition
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.bumptech.glide.Glide
import com.github.twitch4j.chat.events.roomstate.ChannelStatesEvent
import com.github.twitch4j.chat.events.roomstate.Robot9000Event
import com.github.twitch4j.chat.events.roomstate.SlowModeEvent
import com.github.twitch4j.chat.events.roomstate.SubscribersOnlyEvent
import com.github.twitch4j.client.websocket.domain.WebsocketConnectionState
import com.github.twitch4j.helix.domain.Clip
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.google.common.collect.ImmutableMap
import com.perflyst.twire.R
import com.perflyst.twire.activities.stream.LiveStreamActivity
import com.perflyst.twire.adapters.ChatAdapter
import com.perflyst.twire.adapters.ChatAdapter.ChatAdapterCallback
import com.perflyst.twire.chat.ChatManager
import com.perflyst.twire.chat.ChatManager.ChatCallback
import com.perflyst.twire.databinding.FragmentEmoteGridBinding
import com.perflyst.twire.fragments.ChatFragment.EmoteGridFragment.EmoteAdapter.EmoteViewHolder
import com.perflyst.twire.misc.ResizeHeightAnimation
import com.perflyst.twire.model.ChatMessage
import com.perflyst.twire.model.ChatMessage.Companion.getEmotesFromMessage
import com.perflyst.twire.model.Emote
import com.perflyst.twire.model.UserInfo
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings
import com.perflyst.twire.service.Settings.chatAccountConnect
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.keyboardHeight
import com.perflyst.twire.tasks.GetTwitchEmotesTask
import com.perflyst.twire.utils.Constants
import com.perflyst.twire.utils.Execute
import com.perflyst.twire.views.EditTextBackEvent
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.ChatRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.EmoteAutoSpanBehaviour
import dev.chrisbanes.insetter.Insetter
import org.parceler.Parcels
import timber.log.Timber
import java.util.Locale
import java.util.regex.Pattern

internal interface EmoteKeyboardDelegate {
    fun onEmoteClicked(clickedEmote: Emote?, view: View)
}

class ChatFragment : Fragment(), EmoteKeyboardDelegate, ChatAdapterCallback {
    private val vibrationFeedback = HapticFeedbackConstants.KEYBOARD_TAP

    private var chatStatusBarShowing = true

    private var mChatAdapter: ChatAdapter? = null
    private var chatManager: ChatManager? = null
    private var mUserInfo: UserInfo? = null
    private var vodID: String? = null

    private lateinit var mChatInputLayout: RelativeLayout
    private lateinit var mRecyclerView: ChatRecyclerView
    private lateinit var mSendText: EditTextBackEvent
    private lateinit var mSendButton: ImageView
    private lateinit var mSlowmodeIcon: ImageView
    private lateinit var mSubonlyIcon: ImageView
    private lateinit var mR9KIcon: ImageView
    private lateinit var mChatStatus: TextView
    private lateinit var chatInputDivider: View
    private lateinit var mChatStatusBar: FrameLayout

    //Emote Keyboard
    private var textEmotesFragment: EmoteGridFragment? = null
    private var recentEmotesFragment: EmoteGridFragment? = null
    private var twitchEmotesFragment: EmoteGridFragment? = null
    private var customEmotesFragment: EmoteGridFragment? = null
    private var subscriberEmotesFragment: EmoteGridFragment? = null
    private var mEmoteKeyboardButton: ImageView? = null
    private var mEmoteChatBackspace: ImageView? = null
    private var emoteKeyboardContainer: ViewGroup? = null
    private var mEmoteTabs: TabLayout? = null
    private var mEmoteViewPager: ViewPager? = null
    private var selectedTabColorRes: Int? = null
    private var unselectedTabColorRes: Int? = null
    private var keyboardState: KeyboardState? = KeyboardState.CLOSED
    private var defaultBackgroundColor: ColorFilter? = null
    private var bottomSheetDialog: BottomSheetDialog? = null

    private enum class KeyboardState {
        CLOSED,
        SOFT,
        EMOTE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mRootView = inflater.inflate(R.layout.fragment_chat, container, false)
        val context = requireContext()

        val llm = LinearLayoutManager(context)
        llm.setStackFromEnd(true)

        mSendText = mRootView.findViewById(R.id.send_message_textview)
        mSendButton = mRootView.findViewById(R.id.chat_send_ic)
        mSlowmodeIcon = mRootView.findViewById(R.id.slowmode_ic)
        mSubonlyIcon = mRootView.findViewById(R.id.subsonly_ic)
        mR9KIcon = mRootView.findViewById(R.id.r9k_ic)
        mRecyclerView = mRootView.findViewById(R.id.ChatRecyclerView)
        chatInputDivider = mRootView.findViewById(R.id.chat_input_divider)
        mChatInputLayout = mRootView.findViewById(R.id.chat_input)
        mChatInputLayout.bringToFront()
        mChatStatus = mRootView.findViewById(R.id.chat_status_text)
        mChatAdapter = ChatAdapter(mRecyclerView, requireActivity(), this)
        mChatStatusBar = mRootView.findViewById(R.id.chat_status_bar)

        roomStateMap = ImmutableMap.of<Class<out ChannelStatesEvent?>?, ImageView?>(
            SlowModeEvent::class.java, mSlowmodeIcon,
            Robot9000Event::class.java, mR9KIcon,
            SubscribersOnlyEvent::class.java, mSubonlyIcon
        )

        mEmoteKeyboardButton = mRootView.findViewById(R.id.chat_emote_keyboard_ic)
        mEmoteChatBackspace = mRootView.findViewById(R.id.emote_backspace)
        emoteKeyboardContainer = mRootView.findViewById(R.id.emote_keyboard_container)
        mEmoteTabs = mRootView.findViewById(R.id.tabs)
        mEmoteViewPager = mRootView.findViewById(R.id.tabs_viewpager)
        selectedTabColorRes =
            Service.getColorAttribute(R.attr.textColor, R.color.black_text, context)
        unselectedTabColorRes = Service.getColorAttribute(
            R.attr.disabledTextColor,
            R.color.black_text_disabled,
            context
        )

        defaultBackgroundColor = mSendButton.colorFilter
        mRecyclerView.setAdapter(mChatAdapter)
        mRecyclerView.setLayoutManager(llm)
        mRecyclerView.setItemAnimator(null)
        mRecyclerView.setChatPaused(mRootView.findViewById(R.id.chat_paused))

        mUserInfo =
            requireArguments().getParcelable(getString(R.string.stream_fragment_streamerInfo)) // intent.getParcelableExtra(getString(R.string.intent_key_streamer_info));
        vodID = requireArguments().getString(getString(R.string.stream_fragment_vod_id))

        if (!isLoggedIn || vodID != null || !chatAccountConnect) {
            userNotLoggedIn()
        } else {
            setupChatInput()
            loadRecentEmotes()
            setupEmoteViews()
        }

        setupKeyboardShowListener()

        setupTransition()

        Insetter.builder().paddingBottom(WindowInsetsCompat.Type.systemBars(), false)
            .applyToView(mRootView)

        return mRootView
    }

    private var roomStateMap: MutableMap<Class<out ChannelStatesEvent?>?, ImageView?>? = null

    override fun onStart() {
        super.onStart()

        val clip =
            Parcels.unwrap<Clip?>(requireArguments().getParcelable(Constants.KEY_CLIP))
        var vodOffset: Int? = 0
        if (clip != null) {
            vodID = clip.videoId
            vodOffset = clip.vodOffset
            // If the video offset is null, we can't load the chat.
            if (vodOffset == null) {
                showChatStatusBar()
                mChatStatus.text = getString(R.string.clip_chat_expired)
            }
        }

        chatManager = ChatManager(mUserInfo!!, vodID, vodOffset!!, object : ChatCallback {
            val isFragmentActive: Boolean
                get() = !isDetached && isAdded

            override fun onMessage(message: ChatMessage) {
                if (this.isFragmentActive) addMessage(message)
            }

            override fun onClear(target: String?) {
                if (!this.isFragmentActive) return

                if (target == null) {
                    mChatAdapter!!.clear()
                } else {
                    mChatAdapter!!.clear(target)
                }
            }

            override fun onConnectionChanged(state: WebsocketConnectionState) {
                if (!this.isFragmentActive) return

                @StringRes val message: Int? = connectionMap[state]
                if (message == null) return

                showChatStatusBar()
                mChatStatus.setText(message)

                // If connected, show then hide the status bar.
                if (state == WebsocketConnectionState.CONNECTED) hideChatStatusBar()
            }

            override fun onRoomStateChange(event: ChannelStatesEvent) {
                if (!this.isFragmentActive) return

                val icon = roomStateMap!!.getOrDefault(event.javaClass, null)
                if (icon == null) return

                Timber.d("Roomstate has changed")
                icon.setVisibility(if (event.isActive) View.VISIBLE else View.GONE)
                this@ChatFragment.showThenHideChatStatusBar()
            }

            override fun onCustomEmoteIdFetched(
                channel: MutableList<Emote>,
                global: MutableList<Emote>
            ) {
                try {
                    if (this.isFragmentActive) {
                        customEmoteInfoLoaded(channel, global)
                    }
                } catch (e: IllegalAccessError) {
                    Timber.e(e)
                }
            }

            override fun onEmoteSetsFetched(emoteSets: MutableList<String>) {
                val getTwitchEmotesTask = GetTwitchEmotesTask(
                    emoteSets
                ) { twitchEmotes: MutableList<Emote>?, subscriberEmotes: MutableList<Emote>? ->
                    twitchEmotesLoaded(
                        twitchEmotes!!
                    )
                    subscriberEmotesLoaded(
                        subscriberEmotes!!,
                        mEmoteViewPager!!.adapter as EmotesPagerAdapter?
                    )
                }
                Execute.background(getTwitchEmotesTask)
            }
        })

        if (vodOffset != null) Execute.background(chatManager!!)

        if (supportedTextEmotes == null) {
            supportedTextEmotes = ArrayList()
            for (supportedUnicodeEmote in supportedUnicodeEmotes) {
                supportedTextEmotes!!.add(Emote(Character.toString(supportedUnicodeEmote)))
            }
        }
    }


    override fun onPause() {
        super.onPause()
        saveRecentEmotes()

        if (bottomSheetDialog != null) bottomSheetDialog!!.dismiss()
    }

    override fun onStop() {
        super.onStop()
        chatManager!!.stop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun setupTransition() {
        if (activity != null) requireActivity().window.returnTransition
            .addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition?) {
                    mChatStatusBar.visibility = View.GONE
                    mChatStatus.visibility = View.GONE
                }

                override fun onTransitionCancel(transition: Transition?) {
                    onTransitionEnd(transition)
                }

                override fun onTransitionStart(transition: Transition?) {
                    mChatStatusBar.visibility = View.GONE
                    mChatStatus.visibility = View.GONE
                }

                override fun onTransitionPause(transition: Transition?) {
                }

                override fun onTransitionResume(transition: Transition?) {
                }
            })
    }

    private fun showThenHideChatStatusBar() {
        this.showChatStatusBar()
        this.hideChatStatusBar()
    }

    /**
     * Shows the chat status bar with an animation
     */
    private fun showChatStatusBar() {
        if (!this.chatStatusBarShowing) {
            val heightAnimation = ResizeHeightAnimation(
                this.mChatStatusBar,
                resources.getDimension(R.dimen.chat_status_bar_height).toInt()
            )
            heightAnimation.setDuration(240)
            heightAnimation.interpolator = AccelerateDecelerateInterpolator()
            this.mChatStatusBar.startAnimation(heightAnimation)

            this.chatStatusBarShowing = true
        }
    }

    /**
     * Hides the chat status bar with an animation
     */
    private fun hideChatStatusBar() {
        if (this.chatStatusBarShowing) {
            val heightAnimation = ResizeHeightAnimation(this.mChatStatusBar, 0)
            heightAnimation.startOffset = 1000
            heightAnimation.setDuration(140)
            heightAnimation.interpolator = AccelerateDecelerateInterpolator()
            this.mChatStatusBar.startAnimation(heightAnimation)

            this.chatStatusBarShowing = false
        }
    }

    /**
     * Save the recently used emotes
     */
    private fun saveRecentEmotes() {
        if (recentEmotes != null && !recentEmotes!!.isEmpty()) {
            Settings.recentEmotes = recentEmotes
        }
    }

    /**
     * Load the previously used emotes.
     */
    private fun loadRecentEmotes() {
        if (recentEmotes == null) {
            recentEmotes = ArrayList()
            val emotesFromSettings = Settings.recentEmotes
            if (emotesFromSettings != null) {
                recentEmotes!!.addAll(emotesFromSettings)
            } else {
                Timber.e("Failed to load recent emotes")
            }
        }
    }

    /**
     * Checks the recently used emotes and removes any emotes that the user doesn't have access to.
     */
    private fun checkRecentEmotes() {
        if (recentEmotes != null) {
            val emotesToRemove: MutableList<Emote> = ArrayList()
            emotesToHide = ArrayList()

            for (emote in recentEmotes) {
                if (subscriberEmotes != null && emote.isSubscriberEmote && !subscriberEmotes!!.contains(
                        emote
                    )
                ) {
                    emotesToRemove.add(emote)
                } else if (customChannelEmotes != null && emote.isCustomChannelEmote && !customChannelEmotes!!.contains(
                        emote
                    )
                ) {
                    emotesToHide!!.add(emote)
                }
            }

            recentEmotesFragment?.mAdapter.let { mAdapter ->
                if (!emotesToHide!!.isEmpty() && mAdapter != null) {
                    mAdapter.hideEmotes()
                }

                if (!emotesToRemove.isEmpty() && recentEmotesFragment != null) {
                    recentEmotes!!.removeAll(emotesToRemove)

                    mAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * Notify this fragment that back was pressed. Returns true if the super should be called. Else returns false
     */
    fun notifyBackPressed(): Boolean {
        // Check if the emote keyboard is open and close it.
        // The latter condition should never happen but it's been added since you can't detect if the soft keyboard is open reliably.
        if (keyboardState == KeyboardState.EMOTE || (keyboardState == KeyboardState.CLOSED && emoteKeyboardContainer!!.isVisible)) {
            setKeyboardState(KeyboardState.CLOSED)
            return false
        } else {
            return true
        }
    }

    /**
     * Notifies the ChatFragment that the twitch emotes have been fetched.
     * The emotes are added the the twitch emote fragment.
     *
     * @param emotesLoaded The loaded twitch emotes
     */
    private fun twitchEmotesLoaded(emotesLoaded: MutableList<Emote>) {
        twitchEmotes = ArrayList(emotesLoaded)
        if (isLoggedIn) twitchEmotesFragment?.addTwitchEmotes()
    }

    private fun subscriberEmotesLoaded(
        subscriberEmotesLoaded: MutableList<Emote>,
        adapter: EmotesPagerAdapter?
    ) {
        if (!subscriberEmotesLoaded.isEmpty() && adapter != null && context != null) {
            Timber.d("Adding subscriber emotes: %s", subscriberEmotesLoaded.size)

            val icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_attach_money)
            if (icon == null) return
            icon.colorFilter = PorterDuffColorFilter(
                unselectedTabColorRes!!,
                PorterDuff.Mode.SRC_IN
            )

            val newTab = mEmoteTabs!!.newTab()
            newTab.setIcon(icon)
            mEmoteTabs!!.addTab(newTab, adapter.subscribePosition, false)
            adapter.showSubscriberEmote = true
            adapter.notifyDataSetChanged()

            subscriberEmotes = ArrayList(subscriberEmotesLoaded)
            if (isLoggedIn) subscriberEmotesFragment?.addSubscriberEmotes()
        }
        checkRecentEmotes()
    }

    /**
     * Notifies the ChatFragment that the custom emotes have been loaded from the API.
     * Emotes are made and added to the EmoteKeyboard;
     */
    private fun customEmoteInfoLoaded(channel: MutableList<Emote>, global: MutableList<Emote>) {
        Timber.d("Custom Emotes loaded: %s", global.size)
        customChannelEmotes = ArrayList(channel)
        customEmotes = ArrayList(global)
        customEmotes!!.addAll(channel)
        customEmotes!!.sort()

        checkRecentEmotes()
        if (isLoggedIn) customEmotesFragment?.addCustomEmotes()
    }

    private fun setInitialKeyboardHeight() {
        val recordedHeight = keyboardHeight

        if (recordedHeight > 200) {
            val lp = emoteKeyboardContainer!!.layoutParams
            lp.height = recordedHeight
            emoteKeyboardContainer!!.setLayoutParams(lp)
        }
    }

    private fun notifyKeyboardHeightRecorded(keyboardHeight: Int) {
        Timber.d("Keyboard height: %s", keyboardHeight)
        Settings.keyboardHeight = keyboardHeight

        val lp = emoteKeyboardContainer!!.layoutParams
        lp.height = keyboardHeight
        emoteKeyboardContainer!!.setLayoutParams(lp)
    }

    private fun emoteButtonClicked(clickedView: View) {
        clickedView.performHapticFeedback(vibrationFeedback)

        if (keyboardState == KeyboardState.EMOTE) {
            if (mSendText.requestFocus()) {
                setKeyboardState(KeyboardState.SOFT)
            }
        } else {
            setKeyboardState(KeyboardState.EMOTE)
        }
    }

    private fun setKeyboardState(state: KeyboardState) {
        if (keyboardState == state) return

        if (state == KeyboardState.EMOTE) mEmoteKeyboardButton!!.setColorFilter(
            Service.getAccentColor(
                requireContext()
            )
        )
        else mEmoteKeyboardButton!!.colorFilter = defaultBackgroundColor
        requireActivity().window
            .setSoftInputMode(if (emoteKeyboardContainer!!.isVisible) WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        when (state) {
            KeyboardState.EMOTE -> {
                if (keyboardState == KeyboardState.SOFT) toggleKeyboard(false)

                emoteKeyboardContainer!!.visibility = View.VISIBLE
            }

            KeyboardState.SOFT -> {
                toggleKeyboard(true)
            }

            KeyboardState.CLOSED -> {
                emoteKeyboardContainer!!.visibility = View.GONE
                toggleKeyboard(false)
            }
        }

        keyboardState = state
    }

    fun toggleKeyboard(visible: Boolean) {
        val inputMethodManager = ContextCompat.getSystemService(
            requireActivity(),
            InputMethodManager::class.java
        )
        requireActivity().currentFocus?.let { currentFocus ->
            if (visible) inputMethodManager?.showSoftInput(currentFocus, 0)
            else inputMethodManager?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun setupEmoteViews() {
        setInitialKeyboardHeight()
        mEmoteKeyboardButton!!.setOnClickListener { clickedView: View? ->
            this.emoteButtonClicked(
                clickedView!!
            )
        }

        mEmoteChatBackspace!!.setOnClickListener { view: View? ->
            mSendText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            requireView().performHapticFeedback(vibrationFeedback)
        }

        setupEmoteTabs()
    }

    private fun setupEmoteTabs() {
        if (activity == null) return

        val pagerAdapter = EmotesPagerAdapter(requireActivity().supportFragmentManager)

        for (i in 0..<mEmoteTabs!!.tabCount) {
            val tab = mEmoteTabs!!.getTabAt(i)
            val icon = tab?.icon

            if (icon != null) {
                if (i == 0) {
                    icon.colorFilter = PorterDuffColorFilter(
                        selectedTabColorRes!!,
                        PorterDuff.Mode.SRC_IN
                    )
                } else {
                    icon.colorFilter = PorterDuffColorFilter(
                        unselectedTabColorRes!!,
                        PorterDuff.Mode.SRC_IN
                    )
                }
            }
        }

        mEmoteViewPager!!.setAdapter(pagerAdapter)
        mEmoteViewPager!!.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                if (mEmoteTabs!!.tabCount - 1 >= position) {
                    val tab = mEmoteTabs!!.getTabAt(position)
                    tab?.select()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        mEmoteTabs!!.addOnTabSelectedListener(
            object : TabLayout.ViewPagerOnTabSelectedListener(mEmoteViewPager) {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    super.onTabSelected(tab)

                    if (tab.icon != null) tab.icon!!.colorFilter = PorterDuffColorFilter(
                        selectedTabColorRes!!,
                        PorterDuff.Mode.SRC_IN
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    super.onTabUnselected(tab)
                    if (tab.icon != null) tab.icon!!.colorFilter = PorterDuffColorFilter(
                        unselectedTabColorRes!!,
                        PorterDuff.Mode.SRC_IN
                    )
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    super.onTabReselected(tab)
                }
            }
        )
    }

    private val sendText: Editable
        get() = (if (mSendText.getText() == null) Editable.Factory.getInstance()
            .newEditable("") else mSendText.getText())!!

    private fun setupChatInput() {
        mChatInputLayout.bringToFront()
        chatInputDivider.bringToFront()
        mSendText.bringToFront()

        mSendButton.setOnClickListener { v: View? -> sendMessage() }
        mSendText.setOnEditTextImeBackListener { ctrl: EditTextBackEvent?, text: String? ->
            if (keyboardState == KeyboardState.SOFT) setKeyboardState(
                KeyboardState.CLOSED
            )
            setSuggestions(ArrayList())
        }
        mSendText.setOnEditorActionListener(OnEditorActionListener { textView: TextView?, actionId: Int, keyEvent: KeyEvent? ->
            // actionId will be EditorInfo.IME_NULL when pressing the enter key.
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_NULL) {
                sendMessage()
                return@OnEditorActionListener true
            }
            false
        })

        val lastWordPattern = Pattern.compile("(.)([^ ]+)$")
        val fragment = this
        mSendText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                var suggestions: MutableList<String> = ArrayList()

                val matcher = lastWordPattern.matcher(fragment.sendText)
                if (matcher.matches()) {
                    val firstCharacter = matcher.group(1)
                    val lastWord = matcher.group(2)!!.lowercase(Locale.getDefault())
                    if (firstCharacter == "@") {
                        mChatAdapter!!.getNamesThatMatches(lastWord, suggestions)
                    } else if (firstCharacter == ":" && customEmotes != null) {
                        suggestions = sequenceOf(
                            customEmotes!!,
                            twitchEmotes!!,
                            subscriberEmotes!!
                        )
                            .flatten()
                            .map { it.keyword }
                            .filter { it.lowercase(Locale.getDefault()).contains(lastWord) }
                            .distinct()
                            .take(10)
                            .sorted()
                            .toMutableList()
                    }
                }

                setSuggestions(suggestions)
            }
        })

        mSendText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (fragment.sendText.isNotEmpty()) {
                    mSendButton.setColorFilter(Service.getAccentColor(requireContext()))
                    mSendButton.isClickable = true
                } else {
                    mSendButton.colorFilter = defaultBackgroundColor
                    mSendButton.isClickable = false
                }
            }
        })
        mSendText.onFocusChangeListener = OnFocusChangeListener { view: View?, hasFocus: Boolean ->
            if (keyboardState == KeyboardState.EMOTE && hasFocus) {
                setKeyboardState(KeyboardState.SOFT)
            }
        }
    }

    fun insertMentionSuggestion(mention: String) {
        val currentInputText = this.sendText.toString()
        val mentionStart = currentInputText.lastIndexOf('@')
        val newInputText = "${currentInputText.substring(0, mentionStart + 1)}$mention "
        mSendText.setText(newInputText)
        mSendText.setSelection(newInputText.length)
    }

    private fun setSuggestions(suggestions: MutableList<String>) {
        if (activity is LiveStreamActivity && activity != null) {
            val mInputRect = Rect()
            mSendText.getGlobalVisibleRect(mInputRect)
            (activity as LiveStreamActivity).setSuggestions(suggestions, mInputRect)
        }
    }

    private fun userNotLoggedIn() {
        mChatInputLayout.visibility = View.GONE
        chatInputDivider.visibility = View.GONE
    }

    private fun setupKeyboardShowListener() {
        if (activity == null) return

        val mRootWindow = requireActivity().window
        val mRootView2 = mRootWindow.decorView.findViewById<View>(android.R.id.content)
        mRootView2.getViewTreeObserver().addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                var lastBottom: Int = -1

                override fun onGlobalLayout() {
                    try {
                        if (this@ChatFragment.isAdded) {
                            val r = Rect()
                            val view = mRootWindow.decorView
                            view.getWindowVisibleDisplayFrame(r)

                            if (lastBottom > r.bottom && lastBottom - r.bottom > 200 && (resources.configuration.orientation
                                        == Configuration.ORIENTATION_PORTRAIT)
                            ) {
                                Timber.d("Soft Keyboard shown")

                                notifyKeyboardHeightRecorded(lastBottom - r.bottom)
                                setKeyboardState(KeyboardState.SOFT)
                            }
                            lastBottom = r.bottom
                        }
                    } catch (e: IllegalStateException) {
                        Timber.e(e)
                    }
                }
            })
    }


    /**
     * Construct and sends a message through the twitch bot and adds it to the chat recyclerview
     */
    private fun sendMessage() {
        val message = mSendText.getText().toString()
        if (message.isEmpty()) {
            setKeyboardState(KeyboardState.CLOSED)
            return
        }

        mSendButton.performHapticFeedback(vibrationFeedback)

        Timber.d("Sending Message: %s", message)
        val emotes = listOf(customEmotes!!, twitchEmotes!!, subscriberEmotes!!).flatten()
            .associateBy { it.keyword }

        val chatMessage = ChatMessage(
            message,
            chatManager!!.userDisplayName!!,
            chatManager!!.userColor,
            chatManager!!.getBadges(chatManager!!.userBadges!!),
            getEmotesFromMessage(message, emotes),
            false
        )
        try {
            addMessage(chatMessage)
            Timber.d("Message added")
        } catch (e: Exception) {
            Timber.e(e)
        }

        setKeyboardState(KeyboardState.CLOSED)
        mSendText.setText("")

        chatManager!!.sendMessage(message)
    }

    /**
     * Adds a Twitch-message to the recyclerview
     */
    private fun addMessage(message: ChatMessage) {
        mChatAdapter!!.add(message)
    }

    fun clearMessages() {
        if (mChatAdapter != null) mChatAdapter!!.clear()
    }

    /**
     * Called from EmoteGridFragments when an Emote in the emotekeyboard has been clicked
     */
    override fun onEmoteClicked(clickedEmote: Emote?, view: View) {
        view.performHapticFeedback(vibrationFeedback)

        if (clickedEmote != null) {
            val startPosition = mSendText.selectionStart
            var emoteKeyword = clickedEmote.keyword

            if (startPosition != 0 && this.sendText[startPosition - 1] != ' ') {
                emoteKeyword = " $emoteKeyword"
            }

            this.sendText.insert(startPosition, emoteKeyword)

            if (recentEmotesFragment != null) {
                recentEmotesFragment!!.addEmote(clickedEmote)
            }
        }
    }

    override fun onMessageClicked(
        formattedMessage: SpannableStringBuilder?,
        userName: String?,
        message: String?
    ) {
        val v = LayoutInflater.from(context).inflate(R.layout.chat_message_options, null)
        bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog!!.setContentView(v)
        val behavior = BottomSheetBehavior.from<View>(v.parent as View)
        behavior.peekHeight = requireContext().resources.displayMetrics.heightPixels / 3

        bottomSheetDialog!!.setOnDismissListener { dialogInterface: DialogInterface? ->
            behavior.setState(
                BottomSheetBehavior.STATE_COLLAPSED
            )
        }

        val mMessage = v.findViewById<TextView>(R.id.text_chat_message)
        val mMention = v.findViewById<TextView>(R.id.text_mention)
        val mDuplicateMessage = v.findViewById<TextView>(R.id.text_duplicate_message)

        if (vodID != null) {
            mMention.visibility = View.GONE
            mDuplicateMessage.visibility = View.GONE
        }

        mMessage.text = formattedMessage
        mMention.setOnClickListener { view: View? ->
            insertSendText("@$userName")
            bottomSheetDialog!!.dismiss()
        }
        mDuplicateMessage.setOnClickListener { view: View? ->
            insertSendText(message)
            bottomSheetDialog!!.dismiss()
        }

        bottomSheetDialog!!.show()
    }

    private fun insertSendText(message: String?) {
        val insertPosition = mSendText.selectionStart
        val textBefore = this.sendText.toString().substring(0, insertPosition)
        val textAfter = this.sendText.toString().substring(insertPosition)

        mSendText.setText(textBefore + message + textAfter)
        mSendText.setSelection(mSendText.length() - textAfter.length)
    }

    internal enum class EmoteFragmentType {
        UNICODE,
        CUSTOM,
        TWITCH,
        SUBSCRIBER,
        ALL
    }

    class EmoteGridFragment : Fragment() {
        private var mEmoteRecyclerView: AutoSpanRecyclerView? = null
        private var mPromotedEmotesRecyclerView: AutoSpanRecyclerView? = null
        private var fragmentType: EmoteFragmentType? = null
        internal var mAdapter: EmoteAdapter? = null
        private var callback: EmoteKeyboardDelegate? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val binding = FragmentEmoteGridBinding.inflate(inflater)

            mEmoteRecyclerView = binding.emoteRecyclerview
            mPromotedEmotesRecyclerView = binding.promotedEmotesRecyclerview

            mEmoteRecyclerView!!.setBehaviour(EmoteAutoSpanBehaviour())
            mPromotedEmotesRecyclerView!!.setBehaviour(EmoteAutoSpanBehaviour())

            mEmoteRecyclerView!!.setHasFixedSize(false)
            mPromotedEmotesRecyclerView!!.setHasFixedSize(false)

            mAdapter = EmoteAdapter()
            val mPromotedAdapter = EmoteAdapter()

            mEmoteRecyclerView!!.setAdapter(mAdapter)
            mPromotedEmotesRecyclerView!!.setAdapter(mPromotedAdapter)

            when (fragmentType) {
                EmoteFragmentType.UNICODE -> addUnicodeEmotes()
                EmoteFragmentType.ALL -> addRecentEmotes()
                EmoteFragmentType.TWITCH -> addTwitchEmotes()
                EmoteFragmentType.CUSTOM -> addCustomEmotes()
                EmoteFragmentType.SUBSCRIBER -> addSubscriberEmotes()
                null -> {}
            }

            return binding.getRoot()
        }

        internal fun addSubscriberEmotes() {
            if (mAdapter != null && subscriberEmotes != null && mAdapter!!.itemCount == 0) {
                Timber.d("Adding subscriber emotes")
                mAdapter!!.addEmotes(subscriberEmotes!!)
            }
        }

        private fun addUnicodeEmotes() {
            if (supportedTextEmotes != null && mAdapter != null) {
                mAdapter!!.addEmotes(supportedTextEmotes!!)
            }
        }

        internal fun addCustomEmotes() {
            if (customEmotes != null && mAdapter != null && mAdapter!!.itemCount == 0) {
                mAdapter!!.addEmotes(customEmotes!!)
            }
        }

        internal fun addTwitchEmotes() {
            if (twitchEmotes != null && mAdapter != null) {
                mAdapter!!.addEmotes(twitchEmotes!!)
            }
        }

        private fun addRecentEmotes() {
            if (recentEmotes != null && mAdapter != null) {
                mAdapter!!.addEmotes(recentEmotes!!)
            }
        }

        fun addEmote(emote: Emote?) {
            if (mAdapter != null) mAdapter!!.addEmote(emote)
        }

        internal inner class EmoteAdapter internal constructor() :
            RecyclerView.Adapter<EmoteViewHolder?>() {
            private val emotes: ArrayList<Emote> = ArrayList()
            private var columnsFound = false
            private var isDarkTheme = false

            private val emoteClickListener: View.OnClickListener = View.OnClickListener { view ->
                val itemPosition = mEmoteRecyclerView!!.getChildLayoutPosition(view)
                val emoteClicked = emotes[itemPosition]

                if (callback != null) {
                    callback!!.onEmoteClicked(emoteClicked, view)
                }
            }

            private val emoteLongClickListener: OnLongClickListener =
                OnLongClickListener { view ->
                    val itemPosition = mEmoteRecyclerView!!.getChildLayoutPosition(view)
                    val emoteClicked = emotes[itemPosition]

                    Toast.makeText(context, emoteClicked.keyword, Toast.LENGTH_SHORT).show()
                    false
                }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmoteViewHolder {
                val itemView = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.view_emote_showcase, parent, false)

                isDarkTheme = Settings.isDarkTheme

                itemView.setOnClickListener(emoteClickListener)
                itemView.setOnLongClickListener(emoteLongClickListener)
                return EmoteViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: EmoteViewHolder, position: Int) {
                val emoteAtPosition = emotes[position]

                val emoteSize = 2
                val emoteUrl = emoteAtPosition.getEmoteUrl(emoteSize, isDarkTheme)
                if (emoteUrl == null) {
                    holder.mTextEmote.text = emoteAtPosition.keyword
                } else {
                    Glide.with(requireContext()).load(emoteUrl).into(holder.mImageEmote)
                }
            }

            override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
                super.onAttachedToRecyclerView(recyclerView)

                val manager = recyclerView.layoutManager
                if (manager is GridLayoutManager) {
                    val gridLayoutManager = manager

                    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)

                            if (columnsFound) return

                            // To improve performance when scrolling emotes, we'll bump up the max recycled views.
                            recyclerView.recycledViewPool
                                .setMaxRecycledViews(0, gridLayoutManager.spanCount * 2)
                            columnsFound = true
                        }
                    })
                }
            }

            override fun getItemCount(): Int {
                return emotes.size
            }

            fun hideEmotes() {
                val emotesToRemove: MutableList<Emote> = ArrayList()
                for (emote in emotes) {
                    if (emotesToHide!!.contains(emote)) {
                        emotesToRemove.add(emote)
                    }
                }

                emotes.removeAll(emotesToRemove)
                notifyDataSetChanged()
            }

            fun addEmote(emote: Emote?) {
                if (fragmentType == EmoteFragmentType.ALL && emotesToHide != null && emotesToHide!!.contains(
                        emote!!
                    )
                ) {
                    return
                }

                if (!emotes.contains(emote)) {
                    val position = if (fragmentType == EmoteFragmentType.ALL) 0 else emotes.size
                    emotes.add(position, emote!!)
                    notifyItemInserted(position)

                    if (fragmentType == EmoteFragmentType.ALL && recentEmotes != null && !recentEmotes!!.contains(
                            emote
                        )
                    ) {
                        recentEmotes!!.add(position, emote)
                    }
                } else if (!isVisible) {
                    val position = emotes.indexOf(emote)
                    emotes.removeAt(position)
                    notifyItemRemoved(position)
                    addEmote(emote)
                }
            }

            fun addEmotes(emoteList: MutableList<Emote>) {
                emotes.addAll(emoteList)
                if (fragmentType == EmoteFragmentType.ALL && emotesToHide != null) {
                    emotes.removeAll(emotesToHide!!)
                }

                notifyDataSetChanged()
            }

            internal inner class EmoteViewHolder(itemView: View) :
                RecyclerView.ViewHolder(itemView) {
                val mImageEmote: ImageView = itemView.findViewById(R.id.imageEmote)
                val mTextEmote: TextView = itemView.findViewById(R.id.textEmote)
            }
        }

        companion object {
            internal fun newInstance(
                fragmentType: EmoteFragmentType,
                callback: EmoteKeyboardDelegate
            ): EmoteGridFragment {
                val emoteGridFragment = EmoteGridFragment()
                emoteGridFragment.fragmentType = fragmentType
                emoteGridFragment.callback = callback
                return emoteGridFragment
            }

            fun newInstance(): EmoteGridFragment {
                return EmoteGridFragment()
            }
        }
    }

    private inner class EmotesPagerAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        val recentPosition: Int = 0
        val twitchPosition: Int = 1
        val subscribePosition: Int = 2
        val customPosition: Int = 3
        val emojiPosition: Int = 4
        var showSubscriberEmote: Boolean = false

        init {
            val delegate: EmoteKeyboardDelegate = this@ChatFragment

            textEmotesFragment =
                EmoteGridFragment.Companion.newInstance(EmoteFragmentType.UNICODE, delegate)
            recentEmotesFragment =
                EmoteGridFragment.Companion.newInstance(EmoteFragmentType.ALL, delegate)
            twitchEmotesFragment =
                EmoteGridFragment.Companion.newInstance(EmoteFragmentType.TWITCH, delegate)
            subscriberEmotesFragment =
                EmoteGridFragment.Companion.newInstance(EmoteFragmentType.SUBSCRIBER, delegate)
            customEmotesFragment =
                EmoteGridFragment.Companion.newInstance(EmoteFragmentType.CUSTOM, delegate)
        }

        override fun getItem(position: Int): Fragment {
            var position = position
            if (!showSubscriberEmote && position >= subscribePosition) {
                position++
            }

            return when (position) {
                recentPosition -> recentEmotesFragment!!
                twitchPosition -> twitchEmotesFragment!!
                subscribePosition -> subscriberEmotesFragment!!
                customPosition -> customEmotesFragment!!
                emojiPosition -> textEmotesFragment!!
                else -> EmoteGridFragment.Companion.newInstance()
            }
        }

        override fun getCount(): Int {
            var count = emojiPosition + 1
            if (!showSubscriberEmote) {
                count--
            }
            return count
        }
    }

    companion object {
        private val supportedUnicodeEmotes = arrayOf<Int>(
            0x1F600,
            0x1F601,
            0x1F602,
            0x1F603,
            0x1F604,
            0x1F605,
            0x1F606,
            0x1F607,
            0x1F608,
            0x1F609,
            0x1F60A,
            0x1F60B,
            0x1F60C,
            0x1F60D,
            0x1F60E,
            0x1F60F,
            0x1F610,
            0x1F611,
            0x1F612,
            0x1F613,
            0x1F614,
            0x1F615,
            0x1F616,
            0x1F617,
            0x1F618,
            0x1F619,
            0x1F61A,
            0x1F61B,
            0x1F61C,
            0x1F61D,
            0x1F61E,
            0x1F61F,
            0x1F620,
            0x1F621,
            0x1F622,
            0x1F623,
            0x1F624,
            0x1F625,
            0x1F626,
            0x1F627,
            0x1F628,
            0x1F629,
            0x1F62A,
            0x1F62B,
            0x1F62C,
            0x1F62D,
            0x1F62E,
            0x1F62F,
            0x1F630,
            0x1F631,
            0x1F632,
            0x1F633,
            0x1F634,
            0x1F635,
            0x1F636,
            0x1F637,
            0x1F638,
            0x1F639,
            0x1F63A,
            0x1F63B,
            0x1F63C,
            0x1F63D,
            0x1F63E,
            0x1F63F,
            0x1F640,
            0x1F641,
            0x1F642,
            0x1F643,
            0x1F644,
            0x1F645,
            0x1F646,
            0x1F647,
            0x1F648,
            0x1F649,
            0x1F64A,
            0x1F64B,
            0x1F64C,
            0x1F64D,
            0x1F64E,
            0x1F64F
        )

        private var supportedTextEmotes: ArrayList<Emote>? = null
        private var customEmotes: ArrayList<Emote>? = null
        private var customChannelEmotes: ArrayList<Emote>? = null
        private var twitchEmotes: ArrayList<Emote>? = null
        private var subscriberEmotes: ArrayList<Emote>? = null
        private var recentEmotes: ArrayList<Emote>? = null
        private var emotesToHide: ArrayList<Emote>? = null

        @JvmStatic
        fun getInstance(args: Bundle?): ChatFragment {
            val fragment = ChatFragment()
            fragment.setArguments(args)
            return fragment
        }

        private val connectionMap: Map<WebsocketConnectionState, Int> =
            ImmutableMap.of<WebsocketConnectionState, Int>(
                WebsocketConnectionState.DISCONNECTED, R.string.chat_status_connection_failed,
                WebsocketConnectionState.CONNECTING, R.string.chat_status_connecting,
                WebsocketConnectionState.CONNECTED, R.string.chat_status_connected,
                WebsocketConnectionState.LOST, R.string.chat_status_reconnecting
            )
    }
}
