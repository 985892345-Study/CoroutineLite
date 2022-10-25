package com.bennyhuo.kotlin.coroutines.cancel

enum class CancelDecision {
    /**
     * 未准备就绪
     */
    UNDECIDED,

    /**
     * 挂起
     */
    SUSPENDED,

    /**
     * 恢复
     */
    RESUMED
}