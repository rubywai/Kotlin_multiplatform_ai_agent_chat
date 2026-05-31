package com.rubylearner.kmpagent

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform