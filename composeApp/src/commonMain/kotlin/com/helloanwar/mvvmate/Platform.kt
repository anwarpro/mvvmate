package com.helloanwar.mvvmate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform