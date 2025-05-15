package com.perflyst.twire.utils

import android.os.Handler
import android.os.Looper
import androidx.core.os.ExecutorCompat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

object Execute {
    private val uiHandler: Executor = ExecutorCompat.create(Handler(Looper.getMainLooper()))
    private val backgroundExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val listeningExecutor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(backgroundExecutor)

    fun ui(runnable: Runnable) {
        uiHandler.execute(runnable)
    }

    fun background(runnable: Runnable) {
        listeningExecutor.execute(runnable)
    }

    fun <T> background(callable: Callable<T>): ListenableFuture<T> {
        return listeningExecutor.submit(callable)
    }

    fun <T> background(callable: Callable<T>, callback: Consumer<T>): ListenableFuture<T> {
        val isUi = Looper.getMainLooper().thread === Thread.currentThread()

        val future = listeningExecutor.submit(callable)
        Futures.addCallback(future, object : FutureCallback<T> {
            override fun onSuccess(result: T) {
                callback.accept(result)
            }

            override fun onFailure(t: Throwable) {
                if (t is CancellationException) return

                rethrow(t)
            }
        }, if (isUi) uiHandler else listeningExecutor)
        return future
    }

    private fun rethrow(t: Throwable) {
        val uncaughtExceptionHandler = Thread.currentThread().uncaughtExceptionHandler
        uncaughtExceptionHandler?.uncaughtException(
            Thread.currentThread(),
            t
        )
    }
}
