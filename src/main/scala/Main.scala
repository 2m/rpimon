import java.net.Socket

import scala.compiletime.ops.double
import scala.concurrent.duration.*
import scala.scalanative.libc.string
import scala.scalanative.posix.fcntl.*
import scala.scalanative.posix.fcntlOps.*
import scala.scalanative.posix.netdb.*
import scala.scalanative.posix.netdbOps.*
import scala.scalanative.posix.sys.socket.*
import scala.scalanative.posix.unistd.*
import scala.scalanative.runtime.*
import scala.scalanative.unsigned.UByte
import scala.scalanative.unsigned.UInt
import scala.scalanative.unsigned.UShort

import gears.async.*
import gears.async.default.given
import libmqttc.aliases.mqtt_pal_socket_handle
import libmqttc.aliases.uint8_t
import libmqttc.all.*
import libmqttc.enumerations.MQTTConnectFlags
import libmqttc.enumerations.MQTTErrors.MQTT_OK
import libmqttc.structs.mqtt_client
import scalanative.libc.*
import scalanative.unsafe.*
import scalanative.unsigned.*

def open_nb_socket(addr: CString, port: CString): CInt =
  Zone:
    val hints = alloc[addrinfo]()
    hints.ai_family = AF_UNSPEC
    hints.ai_socktype = SOCK_STREAM

    val servinfo = alloc[Ptr[addrinfo]]()
    val rv = getaddrinfo(addr, port, hints, servinfo)
    if rv != 0 then
      println(s"getaddrinfo: ${gai_strerror(rv)}")
      return -1

    // open the first possible socket
    var sockfd = -1
    var p = !servinfo
    var continue = true
    while p != null && continue do
      sockfd = socket(p.ai_family, p.ai_socktype, p.ai_protocol)
      println(sockfd)
      if sockfd != -1 then
        val rv = connect(sockfd, p.ai_addr, p.ai_addrlen)
        println(rv)
        if rv == -1 then
          close(sockfd)
          sockfd = -1
        else continue = false

      if continue then p = p.ai_next

    freeaddrinfo(!servinfo)

    // if sockfd != -1 then fcntl(sockfd, F_SETFL, fcntl(sockfd, F_GETFL, 0) | O_NONBLOCK)

    sockfd

def publish_callback(unused: Ptr[Ptr[Byte]], published: Ptr[mqtt_response_publish]) =
  println(s"published")

def mqttSync(client: Ptr[mqtt_client])(using Async.Spawn) =
  Future:
    while true do
      println("syncing")
      mqtt_sync(client)
      println("after sync")
      AsyncOperations.sleep(500.millis)
      println("after sleep")

@main def hello(): Unit =
  Async.blocking:
    Zone:
      val sockfd = open_nb_socket(c"localhost", c"1883")
      println(s"socket: $sockfd")

      val client = alloc[mqtt_client](1)
      val sendbuf = alloc[UByte](2048)
      val recvbuf = alloc[UByte](2048)

      val callback =
        CFuncPtr2.fromScalaFunction(publish_callback)

      mqtt_init(
        client,
        sockfd.asInstanceOf[mqtt_pal_socket_handle],
        sendbuf,
        Intrinsics.unsignedOf(2048),
        recvbuf,
        Intrinsics.unsignedOf(2048),
        callback
      )
      println("init success")

      mqtt_connect(
        client,
        c"scala-native",
        null,
        null,
        0.toUByte,
        null,
        null,
        2.toUByte,
        400.toUShort
      )
      println("connect success")

      mqttSync(client)

      if (!client).error != MQTT_OK then println(s"connect failed: ${fromCString(mqtt_error_str((!client).error))}")
      println("connect success")
      val msg = c"Hello, world!"
      /* mqtt_publish(
        client,
        c"topic",
        msg,
        string.strlen(msg),
        0.toUByte
      )*/
      println("publish success")
