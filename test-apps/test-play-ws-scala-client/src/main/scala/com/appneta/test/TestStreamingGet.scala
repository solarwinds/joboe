package com.appneta.test

import play.api.libs.ws.WSClient

object TestStreamingGet extends HttpClientTestApp {

  override def test(wsClient: WSClient, url: String) = {
    val future = wsClient.url(url).stream()

    future
  }
}