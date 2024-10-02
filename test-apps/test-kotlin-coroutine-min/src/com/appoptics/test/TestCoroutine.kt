package com.appoptics.test

import com.appoptics.api.ext.AgentChecker
import com.appoptics.api.ext.Trace
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS)

    Trace.startTrace("test-kotlin-coroutine").report()

    runBlocking {
        TimeUnit.SECONDS.sleep(1)
        // Creates a new coroutine scope
        launch {
            longComputation("launch with default context", sleepTime = 1)
        }
        launch(Dispatchers.Unconfined) {
            longComputation("launch with unconfined context", sleepTime = 2)
        }
        launch(Dispatchers.Default) {
            longComputation("launch with default dispatcher", sleepTime = 3)
        }
        launch(newSingleThreadContext("MyOwnThread")) {
            longComputation("launch with single thread context", sleepTime = 4)
        }
        async {
            longComputation("async with default context", sleepTime = 1)
            1
        }
        async(Dispatchers.Unconfined) {
            longComputation("async with unconfined context", sleepTime = 2)
            2
        }
        async(Dispatchers.Default) {
            longComputation("async with default dispatcher", sleepTime = 3)
            3
        }
        async(newSingleThreadContext("MyOwnThread")) {
            longComputation("async with single thread context", sleepTime = 4)
            4
        }

        //test switching context
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    Trace.createEntryEvent("context1").report()
                    delay(1000)
                    val result = withContext(ctx2) {
                        Trace.createEntryEvent("context2").report()
                        delay(500)
                        println("Thread " + Thread.currentThread().id + " Working in ctx2")
                        Trace.createExitEvent("context2").report()
                        "something"
                    }
                    println("Thread " + Thread.currentThread().id + " Back to ctx1 " + result)
                    Trace.createExitEvent("context1").report()
                }
            }
        }

        println("All submitted")
    }
    Trace.endTrace("test-kotlin-coroutine")
}

suspend fun longComputation(label: String, sleepTime: Long): Unit = coroutineScope {
    Trace.createEntryEvent(label.replace(' ', '-')).report()

    val apacheClient = HttpClients.createDefault()
    val result = CompletableFuture.runAsync() {
        apacheClient.execute(HttpGet("http://www.google.com"))
    }

    val ktorClient1 = HttpClient(Apache)

    val deferredResponse1 = async {
        val response = ktorClient1.get<String>("http://www.google.ca")
        response
    }
    val deferredResponse2 = async {
        delay(sleepTime * 1000)
        "done"
    }

    deferredResponse1.await() + deferredResponse2.await()
    result.await()



    Trace.createExitEvent(label.replace(' ', '-')).report()
}



