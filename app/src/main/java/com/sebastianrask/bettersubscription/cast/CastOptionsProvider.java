package com.sebastianrask.bettersubscription.cast;

import android.content.Context;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.sebastianrask.bettersubscription.misc.SecretKeys;

import java.util.List;

/**
 * Created by Sebastian Rask Jepsen on 13/05/2018.
 */

public class CastOptionsProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(SecretKeys.CHROME_CAST_APPLICATION_ID)
                .build();
    }
    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
