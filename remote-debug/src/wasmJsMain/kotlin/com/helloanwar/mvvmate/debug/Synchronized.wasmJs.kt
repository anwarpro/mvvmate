package com.helloanwar.mvvmate.debug

actual inline fun <R> synchronized(lock: Any, block: () -> R): R = block()
