package com.bennyhuo.kotlin.coroutines.sample.test

import com.bennyhuo.kotlin.coroutines.delay
import com.bennyhuo.kotlin.coroutines.exception.CoroutineExceptionHandler
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import java.lang.RuntimeException
import kotlin.concurrent.thread

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email guo985892345@foxmail.com
 * @date 2022/10/25 16:13
 */
fun main() {
  GlobalScope.launch(
    CoroutineExceptionHandler { coroutineContext, throwable ->
      println(throwable.message)
    }
  ) {
    delay(1000)
    throw RuntimeException("launch 异常")
    /*
    * 这里抛出的异常它会直接回调到 resumeWith(Result(RuntimeException))
    * 然后分发到 CoroutineExceptionHandler 中
    * */
  }
  thread {
    while (true);
  }
}