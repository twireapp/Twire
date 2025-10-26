package com.perflyst.twire.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.perflyst.twire.R
import com.perflyst.twire.activities.setup.LoginFragment
import com.perflyst.twire.activities.setup.WelcomeFragment
import com.perflyst.twire.misc.navigate
import com.perflyst.twire.service.Service
import com.perflyst.twire.service.Settings.isLoggedIn
import com.perflyst.twire.service.Settings.isNotificationsDisabled
import com.perflyst.twire.service.Settings.isSetup
import com.perflyst.twire.tasks.ValidateOauthTokenTask
import com.perflyst.twire.utils.Execute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Stack

open class StartUpActivity : ThemeActivity() {
    companion object {
        private const val SINGLE_STACK_KEY = "single_stack"
        private const val SINGLE_STACK_SIZE_KEY = "single_stack_size"
        private const val SINGLE_STACK_CLASS_KEY_PREFIX = "single_stack_class_"
        private const val SINGLE_STACK_ARGS_KEY_PREFIX = "single_stack_args_"
    }

    val singleStack = Stack<Pair<Class<out Fragment>, Bundle?>>()

    protected open suspend fun resolveStartupIntent(): Intent? {
        val fragmentClass: Class<out Fragment> = if (isSetup) {
            Service.getStartPageClass(baseContext)
        } else {
            WelcomeFragment::class.java
        }
        return Intent(this, fragmentClass)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)

        if (isSetup) {
            if (isLoggedIn) {
                validateToken()
            }

            if (!isNotificationsDisabled) {
                Service.startNotifications(baseContext)
            }
        }

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val intent = withContext(Dispatchers.IO) { resolveStartupIntent() }
                if (intent != null) {
                    val fragmentClass = Class.forName(intent.component!!.className) as Class<out Fragment>
                    navigate(fragmentClass, intent.extras, single = true, backStack = false)
                }
            }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (singleStack.size > 1) {
                    singleStack.pop()
                    val data = singleStack.peek()
                    navigate(data.first, data.second, backStack = false)
                } else {
                    finish()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

        supportFragmentManager.addOnBackStackChangedListener {
            callback.isEnabled = supportFragmentManager.backStackEntryCount == 0
        }

        if (savedInstanceState != null) {
            restoreSingleStack(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveSingleStack(outState)
    }

    private fun saveSingleStack(outState: Bundle) {
        val stackBundle = Bundle()
        stackBundle.putInt(SINGLE_STACK_SIZE_KEY, singleStack.size)
        singleStack.forEachIndexed { index: Int, pair: Pair<Class<out Fragment>, Bundle?> ->
            stackBundle.putString(SINGLE_STACK_CLASS_KEY_PREFIX + index, pair.first.name)
            pair.second?.let { stackBundle.putBundle(SINGLE_STACK_ARGS_KEY_PREFIX + index, it) }
        }
        outState.putBundle(SINGLE_STACK_KEY, stackBundle)
    }

    private fun restoreSingleStack(savedInstanceState: Bundle) {
        val stackBundle = savedInstanceState.getBundle(SINGLE_STACK_KEY) ?: return
        val size = stackBundle.getInt(SINGLE_STACK_SIZE_KEY)
        singleStack.clear()
        for (i in 0 until size) {
            val className = stackBundle.getString(SINGLE_STACK_CLASS_KEY_PREFIX + i) ?: continue
            val args = stackBundle.getBundle(SINGLE_STACK_ARGS_KEY_PREFIX + i)
            try {
                val clazz = Class.forName(className) as Class<out Fragment>
                singleStack.push(Pair(clazz, args))
            } catch (e: ClassNotFoundException) {
                Timber.e(e)
            }
        }
    }

    private fun validateToken() {
        Execute.background(ValidateOauthTokenTask()) { validation: String? ->
            if (validation == null) {
                Timber.e("Token invalid")
                val args = bundleOf(
                    getString(R.string.login_intent_part_of_setup) to false,
                    getString(R.string.login_intent_token_not_valid) to true
                )

                navigate(LoginFragment::class.java, args, backStack = false)
            }
        }
    }
}
