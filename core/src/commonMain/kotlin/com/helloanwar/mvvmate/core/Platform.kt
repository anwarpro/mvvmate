package com.helloanwar.mvvmate.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform