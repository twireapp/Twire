package com.sebastianrask.bettersubscription.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.model.ChannelInfo;


public class StreamerInfoFragment extends Fragment {
    private String LOG_TAG = "StreamerInfoFragment";
    private TextView StreamerInfoName, StreamerInfoBio;
    private CardView ContentLayout;
    private ChannelInfo info;
    private Context context;
    private View rootView;

    public StreamerInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_channel_activity_card, container, false);
        context = getActivity().getBaseContext();
        setRetainInstance(true);

        // Get the various handles of view and layouts that is part of this view
        ContentLayout = (CardView) rootView.findViewById(R.id.fragment_streamerInfo_card_layout);
        StreamerInfoName = (TextView) rootView.findViewById(R.id.streamerInfo_displayName);
        StreamerInfoBio = (TextView) rootView.findViewById(R.id.streamerInfo_bio);

        // Get the StreamerInfo object sent with the intent to open this activity
        Intent intent = getActivity().getIntent();
        info = intent.getParcelableExtra("StreamerInfo");

        // Set this Fragment's view with the appropriate information
        StreamerInfoName.setText(info.getDisplayName());
        if(info.getStreamDescription() != null) { // Some times twitch fucks up and doesn't send bio with its API
            StreamerInfoBio.setText(info.getStreamDescription());
        }

        return rootView;

    }

    public void initColorandLayout(int color, int colorLight, int textColor, boolean inLandScape) {
        StreamerInfoName.setTextColor(textColor);
        StreamerInfoName.setBackgroundColor(colorLight);

        if(inLandScape){
            Log.v(LOG_TAG, "Is in landscape - Fragment");
            //StreamerInfoName.getLayoutParams().width = (int) (((getResources().getDisplayMetrics().widthPixels)/100) * StreamerInfoActivity.landscape_content_width - (context.getResources().getDimension(R.dimen.fragment_streamerInfo_cardElevation) * 2));
        } else {
            ContentLayout.setMaxCardElevation(0);
            ContentLayout.setCardElevation(0);
            int screenWidth = (getResources().getDisplayMetrics().widthPixels);
            LinearLayout llContainer = (LinearLayout) rootView.findViewById(R.id.fragment_streamerInfo_container_layout);

            int newWidth = (int)(screenWidth + getResources().getDimension(R.dimen.fragment_streamerInfo_cardElevation));
            llContainer.getLayoutParams().width = newWidth;

            float newTranslationX = (float)((newWidth - screenWidth) * -0.5);
            llContainer.setTranslationX(newTranslationX);
            llContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        }
    }

    public TextView getStreamerInfoName() {
        return StreamerInfoName;
    }
}
