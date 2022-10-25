package com.bennyhuo.kotlin.coroutines.dispatcher

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor

interface Dispatcher {
    fun dispatch(block: ()->Unit)
}

open class DispatcherContext(val dispatcher: Dispatcher) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        println(".(Dispatcher.kt:${Exception().stackTrace[0].lineNumber}) -> " +
          "Dispatcher: ${Thread.currentThread().name}")
        return DispatchedContinuation(continuation, dispatcher)
    }
}

private class DispatchedContinuation<T>(val delegate: Continuation<T>, val dispatcher: Dispatcher) : Continuation<T>{
    override val context = delegate.context

    override fun resumeWith(result: Result<T>) {
        println(".(Dispatcher.kt:${Exception().stackTrace[0].lineNumber}) -> " +
          "DispatchedContinuation: ${Thread.currentThread().name}")
        dispatcher.dispatch {
            delegate.resumeWith(result)
        }
    }
}