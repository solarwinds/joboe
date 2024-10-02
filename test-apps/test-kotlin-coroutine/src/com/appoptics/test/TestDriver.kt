package com.appoptics.test

import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
//    launch { // context of the parent, main runBlocking coroutine
//        println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
//    }
    launch {
        println("Start in thread ${Thread.currentThread().id}")
        val startTime = System.currentTimeMillis()
        delay(500)
        val endTime = System.currentTimeMillis()
        println("After delay in thread ${Thread.currentThread().id}... Delayed for ${endTime - startTime} ms")
    }

    launch {
        println("Start in thread ${Thread.currentThread().id}")
        Thread.sleep(10000)
        println("After delay in thread ${Thread.currentThread().id}")
    }

//    launch(Dispatchers.Default) { // not confined -- will work with main thread
//        println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
//        delay(500)
//        println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
//    }
//    launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher
//        println("Default               : I'm working in thread ${Thread.currentThread().name}")
//    }
//    launch(newSingleThreadContext("MyOwnThread")) { // will get its own new thread
//        println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
//    }
}