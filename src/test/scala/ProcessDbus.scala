/*
 * Copyright 2024 github.com/2m/rpimon/contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rpimon

import cats.effect.IO
import cats.effect.kernel.Resource
import com.softwaremill.diffx.munit.DiffxAssertions
import fs2.Stream
import munit.CatsEffectSuite
import org.legogroup.woof.DefaultLogger
import org.legogroup.woof.Logger
import rpimon.Dbus.*

object ProcessDbusSuite:
  val responses = Map(
    "GetDeviceByIpIface" -> """{"data":["/org/freedesktop/NetworkManager/Devices/3"]}""",
    "org.freedesktop.NetworkManager.Device.Wireless" -> """{"data":[{"AccessPoints":{"data":["/org/freedesktop/NetworkManager/AccessPoint/123456","/org/freedesktop/NetworkManager/AccessPoint/123457","/org/freedesktop/NetworkManager/AccessPoint/non-existent"]},"ActiveAccessPoint":{"data":"/org/freedesktop/NetworkManager/AccessPoint/123456"}}]}""",
    "/org/freedesktop/NetworkManager/AccessPoint/123456" -> """{"data":[{"Ssid":{"data":[36,36]},"Frequency":{"data":5745},"HwAddress":{"data":"00:11:22:33:44:55"},"Strength":{"data":55}},{"Ssid":{"data":[71,105,110,107,117,110,97,105]},"Frequency":{"data":5745},"HwAddress":{"data":"00:11:22:33:44:56"},"Strength":{"data":55}}]}""",
    "/org/freedesktop/NetworkManager/AccessPoint/123457" -> """{"data":[{"Ssid":{"data":[36,36,36]},"Frequency":{"data":5745},"HwAddress":{"data":"00:11:22:33:44:55"},"Strength":{"data":55}},{"Ssid":{"data":[71,105,110,107,117,110,97,105]},"Frequency":{"data":5745},"HwAddress":{"data":"00:11:22:33:44:56"},"Strength":{"data":55}}]}""",
    "/org/freedesktop/NetworkManager/AccessPoint/non-existent" -> "",
    "org.freedesktop.hostname1" -> """{"data":[{"Hostname":{"data":"openmower"},"KernelName":{"data":"Linux"},"KernelRelease":{"data":"6.1.21-v8+"},"OperatingSystemPrettyName":{"data":"Debian GNU/Linux 11 (bullseye)"}}]}"""
  )

  def mockProc[F[_]](responses: Map[String, String]) = new Proc[F]:
    def spawn(command: String, args: String*): Resource[F, Stream[F, Byte]] =
      val stdout = responses
        .collectFirst:
          case (arg, response) if args.contains(arg) => Stream.emits(response.getBytes)
        .getOrElse:
          throw new Error(s"No response from ${responses.keys} found for [$args]")

      Resource.pure[F, Stream[F, Byte]](stdout)

class ProcessDbusSuite extends CatsEffectSuite with DiffxAssertions with Util:
  import ProcessDbusSuite.*

  test("get device"):
    given Proc[IO] = mockProc[IO](responses)
    val dbus = ProcessDbus[IO]

    val d =
      for devicePath <- dbus.devicePath(Device("wlan0"))
      yield devicePath

    d.map(d => assertEqual(d.toString, DevicePath("/org/freedesktop/NetworkManager/Devices/3").toString))

  test("get wireless device"):
    given Proc[IO] = mockProc[IO](responses)
    val dbus = ProcessDbus[IO]

    for
      devicePath <- dbus.devicePath(Device("wlan0"))
      wirelessDevice <- dbus.wirelessDevice(devicePath)
    yield
      assertEqual(wirelessDevice.accessPoints.size, 3)

      assertEqual(
        wirelessDevice.accessPoints.head.toString,
        AccessPointPath("/org/freedesktop/NetworkManager/AccessPoint/123456").toString
      )

  test("get access points"):
    given Proc[IO] = mockProc[IO](responses)
    val dbus = ProcessDbus[IO]

    val d =
      for
        devicePath <- Stream.eval(dbus.devicePath(Device("wlan0")))
        wirelessDevice <- Stream.eval(dbus.wirelessDevice(devicePath))
        accessPointPath <- Stream.emits(wirelessDevice.accessPoints).take(1)
        accessPoint <- Stream.eval(dbus.accessPoint(accessPointPath))
      yield accessPoint

    d.compile.toList.map { accessPoints =>
      assertEqual(accessPoints.size, 1)
      assertEqual(accessPoints.head.ssid.toString, Ssid("$$").toString)
    }

  test("skip unprocessable access points"):
    given Proc[IO] = mockProc[IO](responses)
    given Dbus[IO] = ProcessDbus[IO]

    val d = for
      given Logger[IO] <- Stream.eval(DefaultLogger.makeIo(consoleOutput))
      ApStream(_, apStream) <- apStream[IO]()
      accessPoint <- apStream
    yield accessPoint

    d.compile.toList.map { accessPoints =>
      assertEqual(accessPoints.size, 1)
      assertEqual(accessPoints.head.ssid.toString, Ssid("$$").toString)
    }

  test("only return other access points with the same ssid as the active ap"):
    given Proc[IO] = mockProc[IO](responses)
    given Dbus[IO] = ProcessDbus[IO]

    val d = for
      given Logger[IO] <- Stream.eval(DefaultLogger.makeIo(consoleOutput))
      ApStream(_, apStream) <- apStream[IO]()
      accessPoint <- apStream
    yield accessPoint

    d.compile.toList.map { accessPoints =>
      assertEqual(accessPoints.size, 1)
      assertEqual(accessPoints.head.ssid.toString, Ssid("$$").toString)
    }

  test("get system"):
    given Proc[IO] = mockProc[IO](responses)
    val dbus = ProcessDbus[IO]

    val d = dbus.system()

    d.map { system =>
      assertEqual(system.hostname.toString, Hostname("openmower").toString)
      assertEqual(system.kernelName.toString, KernelName("Linux").toString)
      assertEqual(system.kernelVersion.toString, KernelVersion("6.1.21-v8+").toString)
      assertEqual(system.operatingSystem.toString, OperatingSystem("Debian GNU/Linux 11 (bullseye)").toString)
    }
