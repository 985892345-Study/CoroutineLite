package com.bennyhuo.coroutines.lite

import java.util.concurrent.Executors
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface Dispatcher {
    fun dispatch(block: ()->Unit)
}

private object CommonPoolDispatcher: Dispatcher {
    private val executor = Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors()) { runnable ->
        Thread(runnable, "CommonPool").apply { isDaemon = true }
    }

    override fun dispatch(block: () -> Unit) {
        executor.submit(block)
    }
}

object CommonPool: DispatcherContext(CommonPoolDispatcher)

open class DispatcherContext(private val dispatcher: Dispatcher) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
            = DispatchedContinuation(//下面这段代码是要查找其他拦截器，并保证能调用它们的拦截方法
            continuation.context.fold(continuation, { cont, element ->
                if (element != this && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }), dispatcher)
}

private class DispatchedContinuation<T>(val delegate: Continuation<T>, val dispatcher: Dispatcher) : Continuation<T>{
    override val context = delegate.context

    override fun resumeWith(result: Result<T>) {
        dispatcher.dispatch {
            delegate.resumeWith(result)
        }
    }
}