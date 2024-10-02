package com.example

import spray.json.DefaultJsonProtocol

/**
 * @author pluk
 */
object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat3(Person)
}

case class Person(name: String, fistName: String, age: Long)