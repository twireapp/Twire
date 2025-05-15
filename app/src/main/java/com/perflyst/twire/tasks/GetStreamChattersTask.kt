package com.perflyst.twire.tasks

import com.perflyst.twire.service.Service
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.Callable

/**
 * Created by Sebastian Rask on 22-01-2017.
 */
class GetStreamChattersTask(private val mStreamTwitchName: String) : Callable<ArrayList<String>?> {
    override fun call(): ArrayList<String>? {
        try {
            val BASE_URL = "https://tmi.twitch.tv/group/user/$mStreamTwitchName/chatters"

            val topObject = JSONObject(Service.urlToJSONString(BASE_URL))

            return null
        } catch (e: JSONException) {
            Timber.e(e)
        }
        return null
    }
}
