package com.bennyhuo.kotlin.coroutines.sample.test

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * 调用 createCoroutine() 创建协程时返回的 Continuation<Unit> 的仿写代码
 *
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/10/25 16:29
 */
class StartContinuation<T>(val block: () -> T, val completion: Continuation<T>): Continuation<Unit> {
  
  override val context: CoroutineContext
    get() = completion.context
  
  override fun resumeWith(result: Result<Unit>) {
    // 直接启动协程块
    completion.resumeWith(runCatching { block.invoke() })
  }
}

private fun <T> (() -> T).createCoroutine(
  completion: Continuation<T>
): Continuation<Unit> = StartContinuation(this, completion)

private fun <T> (() -> T).startCoroutine(
  completion: Continuation<T>
) {
  StartContinuation(this, completion).resume(Unit)
}