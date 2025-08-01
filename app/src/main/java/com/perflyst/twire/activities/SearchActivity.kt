package com.perflyst.twire.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.twitch4j.helix.domain.HelixPagination
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.perflyst.twire.R
import com.perflyst.twire.TwireApplication
import com.perflyst.twire.activities.main.LazyFetchingActivity
import com.perflyst.twire.adapters.ChannelsAdapter
import com.perflyst.twire.adapters.GamesAdapter
import com.perflyst.twire.adapters.MainActivityAdapter
import com.perflyst.twire.adapters.StreamsAdapter
import com.perflyst.twire.databinding.ActivitySearchBinding
import com.perflyst.twire.misc.LazyFetchingOnScrollListener
import com.perflyst.twire.misc.Utils
import com.perflyst.twire.model.ChannelInfo
import com.perflyst.twire.model.Game
import com.perflyst.twire.model.StreamInfo
import com.perflyst.twire.views.recyclerviews.AutoSpanRecyclerView
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.AutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.ChannelAutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.GameAutoSpanBehaviour
import com.perflyst.twire.views.recyclerviews.auto_span_behaviours.StreamAutoSpanBehaviour
import com.rey.material.widget.ProgressView
import timber.log.Timber

class SearchActivity : ThemeActivity() {
    private var binding: ActivitySearchBinding? = null
    var query: String? = null
        private set
    private lateinit var mStreamsFragment: SearchFragment<*>
    private lateinit var mChannelsFragment: SearchFragment<*>
    private lateinit var mGamesFragment: SearchFragment<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        setUpTabs()

        binding!!.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                Timber.d("Text changed. Resetting fragments")
                val newQuery = Utils.safeEncode(s.toString())
                mChannelsFragment.reset(newQuery)
                mStreamsFragment.reset(newQuery)
                mGamesFragment.reset(newQuery)
                query = newQuery
            }

            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
        binding!!.icBackArrow.setOnClickListener { v: View? -> onBackPressed() }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in_semi_anim, R.anim.slide_out_bottom_anim)
    }

    private fun setUpTabs() {
        val mViewPager2 = binding!!.searchViewPager2

        // Set up the ViewPager2 with the sections adapter.
        mViewPager2.setAdapter(SearchStateAdapter(this))

        val tabLayout = findViewById<TabLayout>(R.id.search_tabLayout)
        TabLayoutMediator(
            tabLayout,
            mViewPager2
        ) { tab: TabLayout.Tab?, position: Int ->
            when (position) {
                POSITION_STREAMS -> tab!!.setText(R.string.streams_tab)
                POSITION_CHANNELS -> tab!!.setText(R.string.streamers_tab)
                POSITION_GAMES -> tab!!.setText(R.string.games_tab)
                else -> tab!!.setText(R.string.streams_tab)
            }
        }.attach()
    }

    class SearchGamesFragment : SearchFragment<Game>() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun constructBehaviour(): AutoSpanBehaviour {
            return GameAutoSpanBehaviour()
        }

        override fun constructAdapter(): MainActivityAdapter<Game, *> {
            return GamesAdapter(mRecyclerView, requireContext(), requireActivity())
        }

        override fun notifyUserNoElementsAdded() {
        }

        override val visualElements: MutableList<Game>
            get() {
                if (query.isNullOrEmpty()) return mutableListOf()

                val games =
                    TwireApplication.helix.searchCategories(null, query, limit, cursor)
                        .execute().results
                return games.map(::Game).toMutableList()
            }

        companion object {
            fun newInstance(): SearchGamesFragment {
                return SearchGamesFragment()
            }
        }
    }

    class SearchStreamsFragment : SearchFragment<StreamInfo>() {
        override fun constructBehaviour(): AutoSpanBehaviour {
            return StreamAutoSpanBehaviour()
        }

        override fun constructAdapter(): MainActivityAdapter<StreamInfo, *> {
            return StreamsAdapter(mRecyclerView, requireActivity())
        }

        override fun notifyUserNoElementsAdded() {
        }

        override val visualElements: MutableList<StreamInfo>
            get() {
                if (query.isNullOrEmpty()) return mutableListOf()

                val search =
                    TwireApplication.helix.searchChannels(null, query, limit, cursor, true)
                        .execute()
                setCursorFromResponse(search.pagination)

                val streams = TwireApplication.helix.getStreams(
                    null,
                    null,
                    null,
                    limit,
                    null,
                    null,
                    search.results.map { it.id }.toList(),
                    null
                ).execute()
                return streams.streams.map(::StreamInfo).toMutableList()
            }

        companion object {
            fun newInstance(): SearchStreamsFragment {
                return SearchStreamsFragment()
            }
        }
    }

    class SearchChannelsFragment : SearchFragment<ChannelInfo>() {

        override fun constructBehaviour(): AutoSpanBehaviour {
            return ChannelAutoSpanBehaviour()
        }

        override fun constructAdapter(): MainActivityAdapter<ChannelInfo, *> {
            return ChannelsAdapter(mRecyclerView, requireContext(), requireActivity())
        }

        override fun notifyUserNoElementsAdded() {
        }

        override val visualElements: MutableList<ChannelInfo>
            get() {
                if (query.isNullOrEmpty()) return mutableListOf()

                val streams =
                    TwireApplication.helix.searchChannels(null, query, limit, cursor, false)
                        .execute()
                setCursorFromResponse(streams.pagination)
                return streams.results.map(::ChannelInfo).toMutableList()
            }

        companion object {
            fun newInstance(): SearchChannelsFragment {
                return SearchChannelsFragment()
            }
        }
    }

    abstract class SearchFragment<E> : Fragment(), LazyFetchingActivity<E> {
        protected var mAdapter: MainActivityAdapter<E, *>? = null
        protected lateinit var mRecyclerView: AutoSpanRecyclerView
        protected var mProgressView: ProgressView? = null
        var query: String? = null
        private var lazyFetchingOnScrollListener: LazyFetchingOnScrollListener<E>? = null
        override var limit = 20
        override var maxElementsToFetch = 500
        override var cursor: String? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val rootView = inflater.inflate(R.layout.fragment_search, container, false)

            mRecyclerView = rootView.findViewById(R.id.span_recyclerview)
            mProgressView = rootView.findViewById(R.id.circle_progress)

            lazyFetchingOnScrollListener = LazyFetchingOnScrollListener(this)

            setupRecyclerViewAndAdapter()
            checkForQuery()

            // this let keyboard only close when there is a blank page.
            // on swipe or clicking on buttons (stream,games..) won't make keyboard close
            mRecyclerView.setOnTouchListener { v: View?, event: MotionEvent? ->
                val imm = requireActivity().getSystemService(
                    INPUT_METHOD_SERVICE
                ) as InputMethodManager
                imm.hideSoftInputFromWindow(v!!.windowToken, 0)
                false
            }

            return rootView
        }

        private fun setupRecyclerViewAndAdapter() {
            mRecyclerView.setBehaviour(constructBehaviour())
            mRecyclerView.addOnScrollListener(lazyFetchingOnScrollListener!!)
            mRecyclerView.setItemAnimator(null)
            mRecyclerView.setHasFixedSize(true)

            if (mAdapter == null) {
                mAdapter = constructAdapter()
                mAdapter!!.topMarginFirst =
                    resources.getDimension(R.dimen.search_new_adapter_top_margin).toInt()
                mAdapter!!.setSortElements(false)
            }

            mRecyclerView.setAdapter(mAdapter)
        }

        private fun checkForQuery() {
            check(activity is SearchActivity) { "This fragment can only be used with SearchActivity" }

            val activityQuery = (activity as SearchActivity).query
            if (activityQuery != null && activityQuery != query) {
                reset(activityQuery)
            }
        }

        open fun reset(searchQuery: String?) {
            if (mAdapter != null) {
                query = searchQuery
                cursor = null
                mAdapter!!.clearNoAnimation()
                mRecyclerView.scrollToPosition(0)
                startProgress()
                startRefreshing()
                lazyFetchingOnScrollListener!!.resetAndFetch(mRecyclerView)
                mProgressView!!.start()
            }
        }

        protected fun setCursorFromResponse(pagination: HelixPagination) {
            cursor = pagination.cursor
        }

        override fun stopProgress() {
            mProgressView!!.stop()
        }

        override fun startProgress() {
        }

        override fun stopRefreshing() {
        }

        override fun startRefreshing() {
        }

        override fun addToAdapter(aObjectList: MutableList<E>) {
            mAdapter!!.addList(aObjectList)
        }

        abstract fun constructBehaviour(): AutoSpanBehaviour?

        abstract fun constructAdapter(): MainActivityAdapter<E, *>
    }

    private inner class SearchStateAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        init {
            mStreamsFragment = SearchStreamsFragment.Companion.newInstance()
            mChannelsFragment = SearchChannelsFragment.Companion.newInstance()
            mGamesFragment = SearchGamesFragment.Companion.newInstance()
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                POSITION_STREAMS -> mStreamsFragment
                POSITION_CHANNELS -> mChannelsFragment
                POSITION_GAMES -> mGamesFragment
                else -> mStreamsFragment
            }
        }

        override fun getItemCount(): Int {
            return TOTAL_COUNT
        }
    }

    companion object {
        private const val POSITION_STREAMS = 0
        private const val POSITION_CHANNELS = 1
        private const val POSITION_GAMES = 2
        private const val TOTAL_COUNT = 3
    }
}
