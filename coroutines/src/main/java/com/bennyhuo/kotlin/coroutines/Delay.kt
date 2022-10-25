package com.bennyhuo.kotlin.coroutines

import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Created by benny on 2018/5/20.
 */
object Delay {
    val executor = Executors.newScheduledThreadPool(1) { runnable ->
        Thread(runnable, "Scheduler").apply { isDaemon = true }
    }
}

suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) {
    if(time <= 0){
        return
    }

    /**
     * 经过打印 log 验证，调用 suspendCancellableCoroutine 时仍处于上一个线程，
     * 如果没有直接调用 continuation.resume(Unit) 恢复的话，则该次线程执行的 Runnable 直接结束
     * （Runnable 结束不代表 Thread 结束，可能是线程池中的 Thread，则存在 Runnable 队列机制）
     *
     * 而在恢复时调用 continuation.resume(Unit)，会使下游线程切换到调用者的线程，
     * （如：下方方法调用 continuation.resume(Unit)，是在 executor 中调用的，所以下游切换成了 executor 中的线程）
     */
    suspendCancellableCoroutine<Unit> { continuation ->
        println(".(Delay.kt:${Exception().stackTrace[0].lineNumber}) -> " +
          "Delay: suspendCancellableCoroutine -> ${Thread.currentThread().name}")
        val future = Delay.executor.schedule({ continuation.resume(Unit) }, time, unit)
        continuation.invokeOnCancellation { future.cancel(true) }
    }
}