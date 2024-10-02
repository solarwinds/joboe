package com.appneta.test

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

import play.api.libs.ws.WSClient

object TestTimeout extends HttpClientTestApp {

  override def test(wsClient: WSClient, url: String) = {
    val urlWithDuration = url + "?duration=2000"
    wsClient.url(urlWithDuration).withRequestTimeout(Duration(1, TimeUnit.SECONDS)).stream
  }
}