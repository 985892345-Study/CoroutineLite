package com.bennyhuo.coroutines.lite

import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

typealias OnComplete<T> = (T?, Throwable?) -> Unit

sealed class State {
    object InComplete : State()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null) : State()
    class CompleteHandler<T>(val handler: OnComplete<T>) : State()
}

abstract class AbstractCoroutine<T>(override val context: CoroutineContext, block: suspend () -> T) : Continuation<T> {

    protected val state = AtomicReference<State>()

    init {
        state.set(State.InComplete)
        block.startCoroutine(this)
    }

    val isCompleted
        get() = state.get() is State.Complete<*>


    override fun resumeWith(result: Result<T>) {
        result.onSuccess {
            when (val currentState = state.getAndSet(State.Complete(it))) {
                is State.CompleteHandler<*> -> {
                    (currentState as State.CompleteHandler<T>).handler(it, null)
                }
            }
        }.onFailure {
            when (val currentState = state.getAndSet(State.Complete<T>(null, it))) {
                is State.CompleteHandler<*> -> {
                    (currentState as State.CompleteHandler<T>).handler(null, it)
                }
            }
        }
    }

    suspend fun join() {
        val currentState = state.get()
        when (currentState) {
            is State.InComplete -> return joinSuspend()
            is State.Complete<*> -> return
            else -> throw IllegalStateException("Invalid State: $currentState")
        }
    }

    private suspend fun joinSuspend() = suspendCoroutine<Unit> { continuation ->
        doOnCompleted { t, throwable -> continuation.resume(Unit) }
    }

    protected fun doOnCompleted(block: (T?, Throwable?) -> Unit) {
        if (!state.compareAndSet(State.InComplete, State.CompleteHandler<T>(block))) {
            val currentState = state.get()
            when (currentState) {
                is State.Complete<*> -> {
                    (currentState as State.Complete<T>).let {
                        block(currentState.value, currentState.exception)
                    }
                }
                else -> throw IllegalStateException("Invalid State: $currentState")
            }
        }
    }
}