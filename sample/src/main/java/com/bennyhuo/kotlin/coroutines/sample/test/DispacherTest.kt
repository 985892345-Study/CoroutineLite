package com.bennyhuo.kotlin.coroutines.sample.test

import com.bennyhuo.kotlin.coroutines.Delay
import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.dispatcher.Dispatcher
import com.bennyhuo.kotlin.coroutines.dispatcher.DispatcherContext
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * .
 *
 * @author 985892345 (Guo Xiangrui)
 * @date 2022/10/24 11:18
 */
fun main() {
  test1()

  thread {
    // 堵塞主线程，无其他作用
    while (true);
  }
}


private fun test1() {
  GlobalScope.launch(DispatcherContext(NewThreadDispatcher)) {
    println(".(DispacherTest.kt:${Exception().stackTrace[0].lineNumber}) -> " +
              "1: ${Thread.currentThread().name}")
    delay(1000)
    println(".(DispacherTest.kt:${Exception().stackTrace[0].lineNumber}) -> " +
              "2: ${Thread.currentThread().name}")
  }
}

//
/**
 * 上面 test1() 代码我们把它铺开，等同于下面这串代码
 *
 * 可以看到，test2() 的异步回调写起来又臭又长，而采用协程后，回调被铺平了，
 * 并且出现了同一个方法块中竟然运行的线程不同的反常识代码
 */
private fun test2() {
  NewThreadDispatcher.dispatch {
    println(".(DispacherTest.kt:${Exception().stackTrace[0].lineNumber}) -> " +
              "1: ${Thread.currentThread().name}")
    Delay.executor
      .schedule(
        {
          NewThreadDispatcher.dispatch {
            println(".(DispacherTest.kt:${Exception().stackTrace[0].lineNumber}) -> " +
                      "2: ${Thread.currentThread().name}")
          }
        }, 1000, TimeUnit.MILLISECONDS
      )
  }
}

object NewThreadDispatcher : Dispatcher {
  override fun dispatch(block: () -> Unit) {
    thread {
      block()
      println(".(DispacherTest.kt:${Exception().stackTrace[0].lineNumber}) -> " +
        "${Thread.currentThread().name} End")
    }
  }
}