package com.bennyhuo.kotlin.coroutines.cancel

import com.bennyhuo.kotlin.coroutines.CancellationException
import com.bennyhuo.kotlin.coroutines.Job
import com.bennyhuo.kotlin.coroutines.OnCancel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CancellableContinuation<T>(private val continuation: Continuation<T>) : Continuation<T> by continuation {

    private val state = AtomicReference<CancelState>(CancelState.InComplete)
    private val decision = AtomicReference(CancelDecision.UNDECIDED)

    val isCompleted: Boolean
        get() = when (state.get()) {
            CancelState.InComplete,
            is CancelState.CancelHandler -> false
            is CancelState.Complete<*>,
            CancelState.Cancelled -> true
        }

    override fun resumeWith(result: Result<T>) {
        when {
            decision.compareAndSet(CancelDecision.UNDECIDED, CancelDecision.RESUMED) -> {
                /**
                 * 这里说明挂起点没有真正的挂起，稍后会在 getResult() 中返回结果
                 */
                // before getResult called.
                state.set(CancelState.Complete(result.getOrNull(), result.exceptionOrNull()))
            }
            decision.compareAndSet(CancelDecision.SUSPENDED, CancelDecision.RESUMED) -> {
                state.updateAndGet { prev ->
                    when (prev) {
                        is CancelState.Complete<*> -> {
                            throw IllegalStateException("Already completed.")
                        }
                        else -> {
                            CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                        }
                    }
                }
                continuation.resumeWith(result)
            }
        }
    }

    fun getResult(): Any? {
        /**
         * 注册 Cancel 回调
         */
        installCancelHandler()

        if(decision.compareAndSet(CancelDecision.UNDECIDED, CancelDecision.SUSPENDED))
            return COROUTINE_SUSPENDED

        /**
         * 可能它在 block() 时就调用了 resume()，然后恢复了协程
         * 此时因为 getResult() 方法在 block() 后，所以需要进行检测
         */
        return when (val currentState = state.get()) {
            is CancelState.CancelHandler,
            CancelState.InComplete -> COROUTINE_SUSPENDED
            CancelState.Cancelled -> throw CancellationException("Continuation is cancelled.")
            is CancelState.Complete<*> -> {
                (currentState as CancelState.Complete<T>).let {
                    it.exception?.let { throw it } ?: it.value
                }
            }
        }
    }

    private fun installCancelHandler() {
        if (isCompleted) return
        // 这里使用 协程上下文 拿到协程体 Job
        val parent = continuation.context[Job] ?: return
        // 然后注册一个取消的回调
        parent.invokeOnCancel {
            doCancel()
        }
    }

    fun cancel() {
        if (isCompleted) return
        val parent = continuation.context[Job] ?: return
        parent.cancel()
    }

    fun invokeOnCancellation(onCancel: OnCancel) {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> CancelState.CancelHandler(onCancel)
                is CancelState.CancelHandler -> throw IllegalStateException("It's prohibited to register multiple handlers.")
                is CancelState.Complete<*>,
                CancelState.Cancelled -> prev
            }
        }
        if (newState is CancelState.Cancelled) {
            onCancel()
        }
    }

    private fun doCancel() {
        val prevState = state.getAndUpdate { prev ->
            when (prev) {
                is CancelState.CancelHandler,
                CancelState.InComplete -> {
                    CancelState.Cancelled
                }
                CancelState.Cancelled,
                is CancelState.Complete<*> -> {
                    prev
                }
            }
        }
        if (prevState is CancelState.CancelHandler) {
            prevState.onCancel()
            /**
             * 这里直接恢复了挂起点
             */
            resumeWithException(CancellationException("Cancelled."))
            // 之后会回调 override fun resumeWith(result: Result<T>) 方法
        }
    }
}


suspend inline fun <T> suspendCancellableCoroutine(
        crossinline block: (CancellableContinuation<T>) -> Unit
): T = suspendCoroutineUninterceptedOrReturn { continuation ->
    val cancellable = CancellableContinuation(continuation.intercepted())
    block(cancellable)
    /**
     * getResult() 有三种放回状态
     * - 返回 COROUTINE_SUSPENDED 表示挂起
     * - 返回 结果值 表示没有挂起协程，直接继续运行了
     * - 抛出 异常 也表示没有挂起协程
     */
    cancellable.getResult()
}

/**
 * 官方协程挂起点的实现
 */
suspend fun <T> test(): T = suspendCoroutine {

}