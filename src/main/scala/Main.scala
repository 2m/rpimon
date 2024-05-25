package libpahomqtt

import scala.scalanative.unsigned.*

import libpahomqtt.aliases.MQTTClient
import libpahomqtt.aliases.MQTTClient_deliveryToken
import libpahomqtt.extern_functions.MQTTClient_connect
import libpahomqtt.extern_functions.MQTTClient_create
import libpahomqtt.extern_functions.MQTTClient_destroy
import libpahomqtt.extern_functions.MQTTClient_publishMessage
import libpahomqtt.extern_functions.MQTTClient_waitForCompletion
import libpahomqtt.structs.MQTTClient_connectOptions
import libpahomqtt.structs.MQTTClient_message
import scalanative.libc.*
import scalanative.unsafe.*

@link("ssl")
object Main:
  def main(args: Array[String]): Unit =
    Zone:
      println("Hello, world!")
      val client = alloc[MQTTClient](1)
      MQTTClient_create(client, c"tcp://localhost:1883", c"scala-native", 0, null)

      val options = alloc[MQTTClient_connectOptions](1)
      (!options).keepAliveInterval = 20
      (!options).cleansession = 1

      MQTTClient_connect(!client, options)

      val message = alloc[MQTTClient_message](1)
      (!message).payload = c"Hello, world!"
      (!message).payloadlen = 13
      (!message).qos = 0
      (!message).retained = 0

      val token = alloc[MQTTClient_deliveryToken](1)

      MQTTClient_publishMessage(!client, c"topic", message, token)

      MQTTClient_waitForCompletion(!client, !token, 10000.asInstanceOf[USize])

      MQTTClient_destroy(client)
