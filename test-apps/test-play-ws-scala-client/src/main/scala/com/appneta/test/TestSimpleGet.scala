package com.appneta.test

import play.api.libs.ws.WSClient

object TestSimpleGet extends HttpClientTestApp {

  override def test(wsClient: WSClient, url: String) = {
    val future = wsClient.url(url).get

    future
  }
}