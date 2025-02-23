package com.perflyst.twire.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.os.ExecutorCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Execute {
    private static final Executor uiHandler = ExecutorCompat.create(new Handler(Looper.getMainLooper()));
    private static final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();
    private static final ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(backgroundExecutor);

    public static void ui(Runnable runnable) {
        uiHandler.execute(runnable);
    }

    public static void background(Runnable runnable) {
        listeningExecutor.execute(runnable);
    }

    public static <T> ListenableFuture<T> background(Callable<T> callable) {
        return listeningExecutor.submit(callable);
    }

    public static <T> ListenableFuture<T> background(Callable<T> callable, Consumer<T> callback) {
        boolean isUi = Looper.getMainLooper().getThread() == Thread.currentThread();

        var future = listeningExecutor.submit(callable);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                callback.accept(result);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (t instanceof CancellationException)
                    return;

                rethrow(t);
            }
        }, isUi ? uiHandler : listeningExecutor);
        return future;
    }

    private static void rethrow(Throwable t) {
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
        if (uncaughtExceptionHandler != null)
            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
    }
}
