package com.perflyst.twire.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.transition.Transition;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.perflyst.twire.R;
import com.perflyst.twire.activities.stream.LiveStreamActivity;
import com.perflyst.twire.adapters.ChatAdapter;
import com.perflyst.twire.chat.ChatManager;
import com.perflyst.twire.misc.ResizeHeightAnimation;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.ChatMessage;
import com.perflyst.twire.model.Emote;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.ConstructChatMessageTask;
import com.perflyst.twire.tasks.GetTwitchEmotesTask;
import com.perflyst.twire.tasks.SendMessageTask;
import com.perflyst.twire.views.EditTextBackEvent;
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView;
import com.perflyst.twire.views.recyclerviews.ChatRecyclerView;
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.EmoteAutoSpanBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;


interface EmoteKeyboardDelegate {
    void onEmoteClicked(Emote clickedEmote, View view);
}

public class ChatFragment extends Fragment implements EmoteKeyboardDelegate, ChatAdapter.ChatAdapterCallback {
    private static final Integer[] supportedUnicodeEmotes = new Integer[]{
            0x1F600, 0x1F601, 0x1F602, 0x1F603, 0x1F604, 0x1F605, 0x1F606, 0x1F607, 0x1F608, 0x1F609, 0x1F60A, 0x1F60B, 0x1F60C, 0x1F60D, 0x1F60E, 0x1F60F,
            0x1F610, 0x1F611, 0x1F612, 0x1F613, 0x1F614, 0x1F615, 0x1F616, 0x1F617, 0x1F618, 0x1F619, 0x1F61A, 0x1F61B, 0x1F61C, 0x1F61D, 0x1F61E, 0x1F61F,
            0x1F620, 0x1F621, 0x1F622, 0x1F623, 0x1F624, 0x1F625, 0x1F626, 0x1F627, 0x1F628, 0x1F629, 0x1F62A, 0x1F62B, 0x1F62C, 0x1F62D, 0x1F62E, 0x1F62F,
            0x1F630, 0x1F631, 0x1F632, 0x1F633, 0x1F634, 0x1F635, 0x1F636, 0x1F637, 0x1F638, 0x1F639, 0x1F63A, 0x1F63B, 0x1F63C, 0x1F63D, 0x1F63E, 0x1F63F,
            0x1F640, 0x1F641, 0x1F642, 0x1F643, 0x1F644, 0x1F645, 0x1F646, 0x1F647, 0x1F648, 0x1F649, 0x1F64A, 0x1F64B, 0x1F64C, 0x1F64D, 0x1F64E, 0x1F64F
    };

    private static ArrayList<Emote> supportedTextEmotes, customEmotes, customChannelEmotes, twitchEmotes, subscriberEmotes;
    private static ArrayList<Emote> recentEmotes, emotesToHide;

    private final String LOG_TAG = getClass().getSimpleName();
    private final int VIBRATION_FEEDBACK = HapticFeedbackConstants.KEYBOARD_TAP;

    private boolean chatStatusBarShowing = true;

    private ChatAdapter mChatAdapter;
    private ChatManager chatManager;
    private ChannelInfo mChannelInfo;
    private String vodID;
    private Settings settings;

    private RelativeLayout mChatInputLayout;
    private ChatRecyclerView mRecyclerView;
    private EditTextBackEvent mSendText;
    private ImageView mSendButton,
            mSlowmodeIcon,
            mSubonlyIcon,
            mR9KIcon;
    private TextView mChatStatus;
    private View chatInputDivider;
    private FrameLayout mChatStatusBar;

    //Emote Keyboard
    private EmoteGridFragment textEmotesFragment, recentEmotesFragment, twitchEmotesFragment, customEmotesFragment, subscriberEmotesFragment;
    private ImageView mEmoteKeyboardButton, mEmoteChatBackspace;
    private ViewGroup emoteKeyboardContainer;
    private boolean isEmoteKeyboardOpen = false;
    private TabLayout mEmoteTabs;
    private ViewPager mEmoteViewPager;
    private Integer selectedTabColorRes, unselectedTabColorRes;
    private boolean hasSoftKeyboardBeenShown = false,
            hideKeyboardWhenShown = false,
            isSoftKeyboardOpen = false;
    private ColorFilter defaultBackgroundColor;
    private BottomSheetDialog bottomSheetDialog;

    public static ChatFragment getInstance(Bundle args) {
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View mRootView = inflater.inflate(R.layout.fragment_chat, container, false);
        Context context = requireContext();

        LinearLayoutManager llm = new LinearLayoutManager(context);
        llm.setStackFromEnd(true);
        settings = new Settings(context);

        mSendText = mRootView.findViewById(R.id.send_message_textview);
        mSendButton = mRootView.findViewById(R.id.chat_send_ic);
        mSlowmodeIcon = mRootView.findViewById(R.id.slowmode_ic);
        mSubonlyIcon = mRootView.findViewById(R.id.subsonly_ic);
        mR9KIcon = mRootView.findViewById(R.id.r9k_ic);
        mRecyclerView = mRootView.findViewById(R.id.ChatRecyclerView);
        chatInputDivider = mRootView.findViewById(R.id.chat_input_divider);
        mChatInputLayout = mRootView.findViewById(R.id.chat_input);
        mChatInputLayout.bringToFront();
        mChatStatus = mRootView.findViewById(R.id.chat_status_text);
        mChatAdapter = new ChatAdapter(mRecyclerView, getActivity(), this);
        mChatStatusBar = mRootView.findViewById(R.id.chat_status_bar);

        mEmoteKeyboardButton = mRootView.findViewById(R.id.chat_emote_keyboard_ic);
        mEmoteChatBackspace = mRootView.findViewById(R.id.emote_backspace);
        emoteKeyboardContainer = mRootView.findViewById(R.id.emote_keyboard_container);
        mEmoteTabs = mRootView.findViewById(R.id.tabs);
        mEmoteViewPager = mRootView.findViewById(R.id.tabs_viewpager);
        selectedTabColorRes = Service.getColorAttribute(R.attr.textColor, R.color.black_text, context);
        unselectedTabColorRes = Service.getColorAttribute(R.attr.disabledTextColor, R.color.black_text_disabled, context);

        defaultBackgroundColor = mSendButton.getColorFilter();
        mRecyclerView.setAdapter(mChatAdapter);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setItemAnimator(null);

        mChannelInfo = requireArguments().getParcelable(getString(R.string.stream_fragment_streamerInfo));// intent.getParcelableExtra(getResources().getString(R.string.intent_key_streamer_info));
        vodID = requireArguments().getString(getString(R.string.stream_fragment_vod_id));

        if (!settings.isLoggedIn() || vodID != null) {
            userNotLoggedIn();
        } else {
            setupChatInput();
            loadRecentEmotes();
            setupEmoteViews();
        }

        setupKeyboardShowListener();

        setupTransition();
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        final ChatFragment instance = this;
        chatManager = new ChatManager(getContext(), mChannelInfo.getStreamerName(), mChannelInfo.getUserId(), vodID, new ChatManager.ChatCallback() {
            private boolean connected = false;

            private boolean isFragmentActive() {
                return !instance.isDetached() && instance.isAdded();
            }

            @Override
            public void onMessage(ChatMessage message) {
                mRecyclerView.bringToFront();
                if (isFragmentActive())
                    addMessage(message);
            }

            @Override
            public void onConnecting() {
                if (isFragmentActive()) {
                    ChatFragment.this.showChatStatusBar();
                    mChatStatus.setText(getString(R.string.chat_status_connecting));
                }
            }

            @Override
            public void onReconnecting() {
                if (isFragmentActive()) {
                    ChatFragment.this.showChatStatusBar();
                    mChatStatus.setText(getString(R.string.chat_status_reconnecting));
                }
            }

            @Override
            public void onConnected() {
                if (isFragmentActive()) {
                    Log.d(LOG_TAG, "Chat connected");
                    this.connected = true;
                    ChatFragment.this.showThenHideChatStatusBar();
                    mChatStatus.setText(getString(R.string.chat_status_connected));
                }
            }

            @Override
            public void onConnectionFailed() {
                if (isFragmentActive()) {
                    this.connected = false;
                    ChatFragment.this.showChatStatusBar();
                    mChatStatus.setText(getString(R.string.chat_status_connection_failed));
                }
            }

            @Override
            public void onRoomstateChange(boolean isR9K, boolean isSlow, boolean isSubsOnly) {
                if (isFragmentActive()) {
                    if (this.connected) {
                        ChatFragment.this.showThenHideChatStatusBar();
                    } else {
                        ChatFragment.this.showChatStatusBar();
                    }

                    Log.d(LOG_TAG, "Roomstate has changed");
                    this.roomStateIconChange(isR9K, mR9KIcon);
                    this.roomStateIconChange(isSlow, mSlowmodeIcon);
                    this.roomStateIconChange(isSubsOnly, mSubonlyIcon);
                }
            }

            @Override
            public void onCustomEmoteIdFetched(List<Emote> channel, List<Emote> global) {
                try {
                    if (isFragmentActive()) {
                        customEmoteInfoLoaded(channel, global);
                    }
                } catch (IllegalAccessError e) {
                    e.printStackTrace();
                }
            }

            private void roomStateIconChange(boolean isOn, ImageView icon) {
                if (isFragmentActive()) {
                    if (!isOn) {
                        icon.setVisibility(View.GONE);
                    } else {
                        icon.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        if (supportedTextEmotes == null) {
            supportedTextEmotes = new ArrayList<>();
            for (Integer supportedUnicodeEmote : supportedUnicodeEmotes) {
                supportedTextEmotes.add(new Emote(Service.getEmojiByUnicode(supportedUnicodeEmote)));
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        saveRecentEmotes();

        if (bottomSheetDialog != null)
            bottomSheetDialog.dismiss();
    }

    @Override
    public void onStop() {
        super.onStop();
        chatManager.stop();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void setupTransition() {
        if (Build.VERSION.SDK_INT >= 21 && getActivity() != null)
            getActivity().getWindow().getReturnTransition().addListener(new Transition.TransitionListener() {

                @Override
                public void onTransitionEnd(Transition transition) {
                    mChatStatusBar.setVisibility(View.GONE);
                    mChatStatus.setVisibility(View.GONE);
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    onTransitionEnd(transition);
                }

                public void onTransitionStart(Transition transition) {
                    mChatStatusBar.setVisibility(View.GONE);
                    mChatStatus.setVisibility(View.GONE);


                }

                public void onTransitionPause(Transition transition) {
                }

                public void onTransitionResume(Transition transition) {
                }
            });
    }

    private void showThenHideChatStatusBar() {
        this.showChatStatusBar();
        this.hideChatStatusBar();
    }

    /**
     * Shows the chat status bar with an animation
     */
    private void showChatStatusBar() {
        if (!this.chatStatusBarShowing) {
            ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(this.mChatStatusBar, (int) getResources().getDimension(R.dimen.chat_status_bar_height));
            heightAnimation.setDuration(240);
            heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            this.mChatStatusBar.startAnimation(heightAnimation);

            this.chatStatusBarShowing = true;
        }
    }

    /**
     * Hides the chat status bar with an animation
     */
    private void hideChatStatusBar() {
        if (this.chatStatusBarShowing) {
            ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(this.mChatStatusBar, (int) getResources().getDimension(R.dimen.chat_input_divider_height));
            heightAnimation.setStartOffset(1000);
            heightAnimation.setDuration(140);
            heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            this.mChatStatusBar.startAnimation(heightAnimation);

            this.chatStatusBarShowing = false;
        }
    }

    /**
     * Save the recently used emotes
     */
    private void saveRecentEmotes() {
        if (recentEmotes != null && !recentEmotes.isEmpty()) {
            settings.setRecentEmotes(recentEmotes);
        }
    }

    /**
     * Load the previously used emotes.
     */
    private void loadRecentEmotes() {
        if (recentEmotes == null) {
            recentEmotes = new ArrayList<>();
            ArrayList<Emote> emotesFromSettings = settings.getRecentEmotes();
            if (emotesFromSettings != null) {
                recentEmotes.addAll(emotesFromSettings);
            } else {
                Log.e(LOG_TAG, "Failed to load recent emotes");
            }
        }
    }

    /**
     * Checks the recently used emotes and removes any emotes that the user doesn't have access to.
     */
    private void checkRecentEmotes() {
        if (recentEmotes != null) {
            List<Emote> emotesToRemove = new ArrayList<>();
            emotesToHide = new ArrayList<>();

            for (Emote emote : recentEmotes) {
                if (subscriberEmotes != null && emote.isSubscriberEmote() && !subscriberEmotes.contains(emote)) {
                    emotesToRemove.add(emote);
                } else if (customChannelEmotes != null && emote.isCustomChannelEmote() && !customChannelEmotes.contains(emote)) {
                    emotesToHide.add(emote);
                }
            }

            if (emotesToHide.size() > 0 && recentEmotesFragment.mAdapter != null) {
                recentEmotesFragment.mAdapter.hideEmotes();
            }

            if (emotesToRemove.size() > 0 && recentEmotesFragment != null) {
                recentEmotes.removeAll(emotesToRemove);

                if (recentEmotesFragment.mAdapter != null) {
                    recentEmotesFragment.mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    /**
     * Notify this fragment that back was pressed. Returns true if the super should be called. Else returns false
     */
    public boolean notifyBackPressed() {
        if (isEmoteKeyboardOpen) {
            hideEmoteKeyboard();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Notifies the ChatFragment that the twitch emotes have been fetched.
     * The emotes are added the the twitch emote fragment.
     *
     * @param emotesLoaded The loaded twitch emotes
     */
    private void twitchEmotesLoaded(List<Emote> emotesLoaded) {
        twitchEmotes = new ArrayList<>(emotesLoaded);
        if (settings.isLoggedIn() && twitchEmotesFragment != null) {
            twitchEmotesFragment.addTwitchEmotes();
        }
    }

    private void subscriberEmotesLoaded(List<Emote> subscriberEmotesLoaded, EmotesPagerAdapter adapter) {
        if (subscriberEmotesLoaded.size() > 0 && adapter != null && getContext() != null) {
            Log.d(LOG_TAG, "Adding subscriber emotes: " + subscriberEmotesLoaded.size());

            Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_money);
            if (icon == null) return;
            icon.setColorFilter(new PorterDuffColorFilter(unselectedTabColorRes, PorterDuff.Mode.SRC_IN));

            TabLayout.Tab newTab = mEmoteTabs.newTab();
            newTab.setIcon(icon);
            mEmoteTabs.addTab(newTab, adapter.SUBSCRIBE_POSITION, false);
            adapter.showSubscriberEmote = true;
            adapter.notifyDataSetChanged();

            subscriberEmotes = new ArrayList<>(subscriberEmotesLoaded);
            if (settings.isLoggedIn() && subscriberEmotesFragment != null) {
                subscriberEmotesFragment.addSubscriberEmotes();
            }
        }
        checkRecentEmotes();
    }

    /**
     * Notifies the ChatFragment that the custom emotes have been loaded from the API.
     * Emotes are made and added to the EmoteKeyboard;
     */
    private void customEmoteInfoLoaded(List<Emote> channel, List<Emote> global) {
        Log.d(LOG_TAG, "Custom Emotes loaded: " + global.size());
        customChannelEmotes = new ArrayList<>(channel);
        customEmotes = new ArrayList<>(global);
        customEmotes.addAll(channel);
        Collections.sort(customEmotes);

        checkRecentEmotes();
        if (settings.isLoggedIn() && customEmotesFragment != null) {
            customEmotesFragment.addCustomEmotes();
        }
    }

    private void setInitialKeyboardHeight() {
        int recordedHeight = settings.getKeyboardHeight();

        if (recordedHeight > 200) {
            ViewGroup.LayoutParams lp = emoteKeyboardContainer.getLayoutParams();
            lp.height = recordedHeight;
            emoteKeyboardContainer.setLayoutParams(lp);
        }
    }

    private void notifyKeyboardHeightRecorded(int keyboardHeight) {
        Log.d(LOG_TAG, "Keyboard height: " + keyboardHeight);
        settings.setKeyboardHeight(keyboardHeight);

        ViewGroup.LayoutParams lp = emoteKeyboardContainer.getLayoutParams();
        lp.height = keyboardHeight;
        emoteKeyboardContainer.setLayoutParams(lp);

        hasSoftKeyboardBeenShown = true;
    }

    private void emoteButtonClicked(View clickedView) {
        clickedView.performHapticFeedback(VIBRATION_FEEDBACK);

        if (hasSoftKeyboardBeenShown) {
            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        if (!isEmoteKeyboardOpen) {
            if (!hasSoftKeyboardBeenShown) {
                Log.d(LOG_TAG, "SHOW SOFT KEYBOARD");
                hideKeyboardWhenShown = true;
                if (mSendText.requestFocus()) {
                    openSoftKeyboard();
                }
            }

            showEmoteKeyboard();

        } else {
            if (isSoftKeyboardOpen) {
                closeSoftKeyboard();
            } else {
                requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                openSoftKeyboard();
            }
        }
    }

    private void showEmoteKeyboard() {
        Log.d(LOG_TAG, "Show emote keyboard");

        closeSoftKeyboard();
        isEmoteKeyboardOpen = true;
        emoteKeyboardContainer.setVisibility(View.VISIBLE);
        mEmoteKeyboardButton.setColorFilter(Service.getAccentColor(requireContext()));
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    private void hideEmoteKeyboard() {
        Log.d(LOG_TAG, "Hide emote keyboard");
        isEmoteKeyboardOpen = false;

        emoteKeyboardContainer.setVisibility(View.GONE);
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mEmoteKeyboardButton.setColorFilter(defaultBackgroundColor);
    }

    private void openSoftKeyboard() {
        Service.showKeyboard(requireActivity());
        isSoftKeyboardOpen = true;
        mEmoteKeyboardButton.setColorFilter(defaultBackgroundColor);
    }

    private void closeSoftKeyboard() {
        isSoftKeyboardOpen = false;
        Service.hideKeyboard(requireActivity());

        if (isEmoteKeyboardOpen) {
            mEmoteKeyboardButton.setColorFilter(Service.getAccentColor(requireContext()));
        }
    }

    private void setupEmoteViews() {
        setInitialKeyboardHeight();
        mEmoteKeyboardButton.setOnClickListener(this::emoteButtonClicked);

        mEmoteChatBackspace.setOnClickListener(view -> {
            mSendText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            view.performHapticFeedback(VIBRATION_FEEDBACK);
        });

        setupEmoteTabs();
    }

    private void setupEmoteTabs() {
        if (getActivity() == null)
            return;

        final EmotesPagerAdapter pagerAdapter = new EmotesPagerAdapter(getActivity().getSupportFragmentManager());

        for (int i = 0; i < mEmoteTabs.getTabCount(); i++) {
            TabLayout.Tab tab = mEmoteTabs.getTabAt(i);
            Drawable icon = tab != null ? tab.getIcon() : null;

            if (icon != null) {
                if (i == 0) {
                    icon.setColorFilter(new PorterDuffColorFilter(selectedTabColorRes, PorterDuff.Mode.SRC_IN));
                } else {
                    icon.setColorFilter(new PorterDuffColorFilter(unselectedTabColorRes, PorterDuff.Mode.SRC_IN));
                }
            }
        }

        mEmoteViewPager.setAdapter(pagerAdapter);
        mEmoteViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (mEmoteTabs.getTabCount() - 1 >= position) {
                    TabLayout.Tab tab = mEmoteTabs.getTabAt(position);
                    if (tab != null)
                        tab.select();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mEmoteTabs.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(mEmoteViewPager) {
                    @Override
                    public void onTabSelected(@NonNull TabLayout.Tab tab) {
                        super.onTabSelected(tab);

                        if (tab.getIcon() != null)
                            tab.getIcon().setColorFilter(new PorterDuffColorFilter(selectedTabColorRes, PorterDuff.Mode.SRC_IN));
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        if (tab.getIcon() != null)
                            tab.getIcon().setColorFilter(new PorterDuffColorFilter(unselectedTabColorRes, PorterDuff.Mode.SRC_IN));
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                    }
                }
        );

        GetTwitchEmotesTask getTwitchEmotesTask = new GetTwitchEmotesTask((twitchEmotes, subscriberEmotes) -> {
            twitchEmotesLoaded(twitchEmotes);
            subscriberEmotesLoaded(subscriberEmotes, pagerAdapter);
        }, getContext());
        getTwitchEmotesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @NonNull
    private Editable getSendText() {
        return mSendText.getText() == null ? Editable.Factory.getInstance().newEditable("") : mSendText.getText();
    }

    private void setupChatInput() {
        mChatInputLayout.bringToFront();
        chatInputDivider.bringToFront();
        mSendText.bringToFront();

        mSendButton.setOnClickListener(v -> sendMessage());
        mSendText.setOnEditTextImeBackListener((ctrl, text) -> {
            if (isEmoteKeyboardOpen && isSoftKeyboardOpen) {
                hideEmoteKeyboard();
            }

            setMentionSuggestions(new ArrayList<>());
        });
        mSendText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        final Pattern mentionPattern = Pattern.compile("@(\\w+)$");
        mSendText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                Matcher mInputMatcher = mentionPattern.matcher(getSendText());

                String userName = null;
                while (mInputMatcher.find()) {
                    userName = mInputMatcher.group(1);
                }

                if (userName != null && !userName.isEmpty()) {
                    setMentionSuggestions(mChatAdapter.getNamesThatMatches(userName));
                } else {
                    setMentionSuggestions(new ArrayList<>());
                }
            }
        });

        mSendText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (getSendText().length() > 0) {
                    mSendButton.setColorFilter(Service.getAccentColor(requireContext()));
                    mSendButton.setClickable(true);
                } else {
                    mSendButton.setColorFilter(defaultBackgroundColor);
                    mSendButton.setClickable(false);
                }
            }
        });
        mSendText.setOnClickListener(view -> {
            if (isEmoteKeyboardOpen) {
                requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                isSoftKeyboardOpen = true;
                mEmoteKeyboardButton.setColorFilter(defaultBackgroundColor);
            }
        });
    }

    public void insertMentionSuggestion(String mention) {
        String currentInputText = getSendText().toString();
        int mentionStart = currentInputText.lastIndexOf('@');
        String newInputText = currentInputText.substring(0, mentionStart + 1) + mention + " ";
        mSendText.setText(newInputText);
        mSendText.setSelection(newInputText.length());
    }

    private void setMentionSuggestions(List<String> suggestions) {
        if (getActivity() instanceof LiveStreamActivity && getActivity() != null) {
            Rect mInputRect = new Rect();
            mSendText.getGlobalVisibleRect(mInputRect);
            ((LiveStreamActivity) getActivity()).setMentionSuggestions(suggestions, mInputRect);
        }
    }

    private void userNotLoggedIn() {
        mChatInputLayout.setVisibility(View.GONE);
        chatInputDivider.setVisibility(View.GONE);
    }

    private void setupKeyboardShowListener() {
        if (getActivity() == null)
            return;

        final Window mRootWindow = getActivity().getWindow();
        View mRootView2 = mRootWindow.getDecorView().findViewById(android.R.id.content);
        mRootView2.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    Integer lastBottom = -1;

                    public void onGlobalLayout() {
                        try {
                            if (ChatFragment.this.isAdded()) {
                                Rect r = new Rect();
                                View view = mRootWindow.getDecorView();
                                view.getWindowVisibleDisplayFrame(r);

                                if (lastBottom > r.bottom && (lastBottom - r.bottom) > 200 &&
                                        getResources().getConfiguration().orientation
                                                == Configuration.ORIENTATION_PORTRAIT) {
                                    Log.d(LOG_TAG, "Soft Keyboard shown");
                                    if (hideKeyboardWhenShown) {
                                        closeSoftKeyboard();
                                        hideKeyboardWhenShown = false;
                                    }

                                    notifyKeyboardHeightRecorded(lastBottom - r.bottom);
                                }
                                lastBottom = r.bottom;
                            }
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    /**
     * Construct and sends a message through the twitch bot and adds it to the chat recyclerview
     */
    private void sendMessage() {
        final String message = mSendText.getText() + "";
        if (message.isEmpty()) {
            hideEmoteKeyboard();
            closeSoftKeyboard();
            return;
        }

        mSendButton.performHapticFeedback(VIBRATION_FEEDBACK);

        Log.d(LOG_TAG, "Sending Message: " + message);
        ConstructChatMessageTask getMessageTask = new ConstructChatMessageTask(
                chatMessage -> {
                    if (chatMessage != null) {
                        try {
                            addMessage(chatMessage);
                            Log.d(LOG_TAG, "Message added");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                customEmotes,
                twitchEmotes,
                subscriberEmotes,
                chatManager,
                message
        );
        getMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        hideEmoteKeyboard();
        closeSoftKeyboard();
        mSendText.setText("");

        SendMessageTask sendMessageTask = new SendMessageTask(chatManager, message);
        sendMessageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Adds a Twitch-message to the recyclerview
     */
    private void addMessage(ChatMessage message) {
        mChatAdapter.add(message);
    }

    public void clearMessages() {
        if (mChatAdapter != null)
            mChatAdapter.clear();
    }

    /**
     * Called from EmoteGridFragments when an Emote in the emotekeyboard has been clicked
     */
    @Override
    public void onEmoteClicked(Emote clickedEmote, View view) {
        view.performHapticFeedback(VIBRATION_FEEDBACK);

        if (clickedEmote != null) {
            int startPosition = mSendText.getSelectionStart();
            String emoteKeyword = clickedEmote.getKeyword();

            if (startPosition != 0 && getSendText().charAt(startPosition - 1) != ' ') {
                emoteKeyword = " " + emoteKeyword;
            }

            getSendText().insert(startPosition, emoteKeyword);

            if (recentEmotesFragment != null) {
                recentEmotesFragment.addEmote(clickedEmote);
            }
        }
    }

    @Override
    public void onMessageClicked(SpannableStringBuilder formattedMessage, final String userName, final String message) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.chat_message_options, null);
        bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(v);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from((View) v.getParent());
        behavior.setPeekHeight(getContext().getResources().getDisplayMetrics().heightPixels / 3);

        bottomSheetDialog.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        TextView mMessage = v.findViewById(R.id.text_chat_message);
        TextView mMention = v.findViewById(R.id.text_mention);
        TextView mDuplicateMessage = v.findViewById(R.id.text_duplicate_message);

        mMessage.setText(formattedMessage);
        mMention.setOnClickListener(view -> {
            insertSendText("@" + userName);
            bottomSheetDialog.dismiss();
        });
        mDuplicateMessage.setOnClickListener(view -> {
            insertSendText(message);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void insertSendText(String message) {
        int insertPosition = mSendText.getSelectionStart();
        String textBefore = getSendText().toString().substring(0, insertPosition);
        String textAfter = getSendText().toString().substring(insertPosition);

        mSendText.setText(textBefore + message + textAfter);
        mSendText.setSelection(mSendText.length() - textAfter.length());
    }

    protected enum EmoteFragmentType {
        UNICODE,
        CUSTOM,
        TWITCH,
        SUBSCRIBER,
        ALL
    }

    public static class EmoteGridFragment extends Fragment {
        private final String LOG_TAG = getClass().getSimpleName();
        @BindView(R.id.emote_recyclerview)
        protected AutoSpanRecyclerView mEmoteRecyclerView;
        @BindView(R.id.promoted_emotes_recyclerview)
        protected AutoSpanRecyclerView mPromotedEmotesRecyclerView;
        private EmoteFragmentType fragmentType;
        private EmoteAdapter mAdapter;
        private EmoteKeyboardDelegate callback;

        static EmoteGridFragment newInstance(EmoteFragmentType fragmentType, EmoteKeyboardDelegate callback) {
            EmoteGridFragment emoteGridFragment = new EmoteGridFragment();
            emoteGridFragment.fragmentType = fragmentType;
            emoteGridFragment.callback = callback;
            return emoteGridFragment;
        }

        static EmoteGridFragment newInstance() {
            return new EmoteGridFragment();
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View mRootView = inflater.inflate(R.layout.fragment_emote_grid, null);
            ButterKnife.bind(this, mRootView);

            mEmoteRecyclerView.setBehaviour(new EmoteAutoSpanBehaviour());
            mPromotedEmotesRecyclerView.setBehaviour(new EmoteAutoSpanBehaviour());

            mEmoteRecyclerView.setHasFixedSize(false);
            mPromotedEmotesRecyclerView.setHasFixedSize(false);

            mAdapter = new EmoteAdapter();
            EmoteAdapter mPromotedAdapter = new EmoteAdapter();

            mEmoteRecyclerView.setAdapter(mAdapter);
            mPromotedEmotesRecyclerView.setAdapter(mPromotedAdapter);

            if (fragmentType != null) {
                switch (fragmentType) {
                    case UNICODE:
                        addUnicodeEmotes();
                        break;
                    case ALL:
                        addRecentEmotes();
                        break;
                    case TWITCH:
                        addTwitchEmotes();
                        break;
                    case CUSTOM:
                        addCustomEmotes();
                        break;
                    case SUBSCRIBER:
                        addSubscriberEmotes();
                        break;
                }
            }

            return mRootView;
        }

        private void addSubscriberEmotes() {
            if (mAdapter != null && subscriberEmotes != null && mAdapter.getItemCount() == 0) {
                Log.d(LOG_TAG, "Adding subscriber emotes");
                mAdapter.addEmotes(subscriberEmotes);
            }
        }

        private void addUnicodeEmotes() {
            if (supportedTextEmotes != null && mAdapter != null) {
                mAdapter.addEmotes(supportedTextEmotes);
            }
        }

        private void addCustomEmotes() {
            if (customEmotes != null && mAdapter != null && mAdapter.getItemCount() == 0) {
                mAdapter.addEmotes(customEmotes);
            }
        }

        private void addTwitchEmotes() {
            if (twitchEmotes != null && mAdapter != null) {
                mAdapter.addEmotes(twitchEmotes);
            }
        }

        private void addRecentEmotes() {
            if (recentEmotes != null && mAdapter != null) {
                mAdapter.addEmotes(recentEmotes);
            }
        }

        void addEmote(Emote emote) {
            if (mAdapter != null)
                mAdapter.addEmote(emote);
        }

        public class EmoteAdapter extends RecyclerView.Adapter<EmoteAdapter.EmoteViewHolder> {
            private final ArrayList<Emote> emotes;

            private final View.OnClickListener emoteClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int itemPosition = mEmoteRecyclerView.getChildLayoutPosition(view);
                    Emote emoteClicked = emotes.get(itemPosition);

                    if (callback != null) {
                        callback.onEmoteClicked(emoteClicked, view);
                    }
                }
            };

            private final View.OnLongClickListener emoteLongClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int itemPosition = mEmoteRecyclerView.getChildLayoutPosition(view);
                    Emote emoteClicked = emotes.get(itemPosition);

                    Toast.makeText(getContext(), emoteClicked.getKeyword(), Toast.LENGTH_SHORT).show();
                    return false;
                }
            };

            EmoteAdapter() {
                emotes = new ArrayList<>();
            }

            @Override
            @NonNull
            public EmoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.view_emote_showcase, parent, false);

                itemView.setOnClickListener(emoteClickListener);
                itemView.setOnLongClickListener(emoteLongClickListener);
                return new EmoteViewHolder(itemView);
            }

            @Override
            public void onBindViewHolder(@NonNull final EmoteViewHolder holder, int position) {
                final Emote emoteAtPosition = emotes.get(position);

                if (emoteAtPosition.isTextEmote()) {
                    holder.mTextEmote.setText(emoteAtPosition.getKeyword());
                } else {
                    int EMOTE_SIZE = 2;
                    String emoteUrl = emoteAtPosition.getEmoteUrl(EMOTE_SIZE);

                    Glide.with(requireContext()).load(emoteUrl).into(holder.mImageEmote);
                }
            }

            @Override
            public int getItemCount() {
                return emotes.size();
            }

            void hideEmotes() {
                List<Emote> emotesToRemove = new ArrayList<>();
                for (Emote emote : emotes) {
                    if (emotesToHide.contains(emote)) {
                        emotesToRemove.add(emote);
                    }
                }

                emotes.removeAll(emotesToRemove);
                notifyDataSetChanged();
            }

            void addEmote(Emote emote) {
                if (fragmentType == EmoteFragmentType.ALL && emotesToHide != null && emotesToHide.contains(emote)) {
                    return;
                }

                if (!emotes.contains(emote)) {
                    int position = fragmentType == EmoteFragmentType.ALL ? 0 : emotes.size();
                    emotes.add(position, emote);
                    notifyItemInserted(position);

                    if (fragmentType == EmoteFragmentType.ALL && recentEmotes != null && !recentEmotes.contains(emote)) {
                        recentEmotes.add(position, emote);
                    }
                } else if (!isVisible()) {
                    int position = emotes.indexOf(emote);
                    emotes.remove(position);
                    notifyItemRemoved(position);
                    addEmote(emote);
                }
            }

            void addEmotes(List<Emote> emoteList) {
                emotes.addAll(emoteList);
                if (fragmentType == EmoteFragmentType.ALL && emotesToHide != null) {
                    emotes.removeAll(emotesToHide);
                }

                notifyDataSetChanged();
            }

            class EmoteViewHolder extends RecyclerView.ViewHolder {
                final ImageView mImageEmote;
                final TextView mTextEmote;

                EmoteViewHolder(View itemView) {
                    super(itemView);
                    mImageEmote = itemView.findViewById(R.id.imageEmote);
                    mTextEmote = itemView.findViewById(R.id.textEmote);
                }
            }
        }
    }

    private class EmotesPagerAdapter extends FragmentPagerAdapter {
        final int RECENT_POSITION = 0,
                TWITCH_POSITION = 1,
                SUBSCRIBE_POSITION = 2,
                CUSTOM_POSITION = 3,
                EMOJI_POSITION = 4;
        boolean showSubscriberEmote = false;

        EmotesPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            EmoteKeyboardDelegate delegate = ChatFragment.this;

            textEmotesFragment = EmoteGridFragment.newInstance(EmoteFragmentType.UNICODE, delegate);
            recentEmotesFragment = EmoteGridFragment.newInstance(EmoteFragmentType.ALL, delegate);
            twitchEmotesFragment = EmoteGridFragment.newInstance(EmoteFragmentType.TWITCH, delegate);
            subscriberEmotesFragment = EmoteGridFragment.newInstance(EmoteFragmentType.SUBSCRIBER, delegate);
            customEmotesFragment = EmoteGridFragment.newInstance(EmoteFragmentType.CUSTOM, delegate);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            if (!showSubscriberEmote && position >= SUBSCRIBE_POSITION) {
                position++;
            }

            switch (position) {
                case RECENT_POSITION:
                    return recentEmotesFragment;
                case TWITCH_POSITION:
                    return twitchEmotesFragment;
                case SUBSCRIBE_POSITION:
                    return subscriberEmotesFragment;
                case CUSTOM_POSITION:
                    return customEmotesFragment;
                case EMOJI_POSITION:
                    return textEmotesFragment;
                default:
                    return EmoteGridFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            int count = EMOJI_POSITION + 1;
            if (!showSubscriberEmote) {
                count--;
            }
            return count;
        }
    }

}
