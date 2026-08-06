package com.liubimba.debut

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
