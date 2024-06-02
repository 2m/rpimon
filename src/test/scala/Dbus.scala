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

import com.softwaremill.diffx.Diff
import com.softwaremill.diffx.munit.DiffxAssertions
import io.circe.parser.*

class DbusParserSuite extends munit.FunSuite with DiffxAssertions with Util:
  import Dbus.*

  test("parse operating system"):
    val json = file("system.json")
    decode[Dbus.System](json) match
      case Right(os) =>
        assertEqual(os.hostname.toString, Dbus.Hostname("openmower").toString)
        assertEqual(os.kernelName.toString, Dbus.KernelName("Linux").toString)
        assertEqual(os.kernelVersion.toString, Dbus.KernelVersion("6.1.21-v8+").toString)
        assertEqual(os.operatingSystem.toString, Dbus.KernelVersion("Debian GNU/Linux 11 (bullseye)").toString)
      case Left(error) => fail(s"Failed to parse operating system: $error")

  test("parse device path"):
    val json = file("device.json")
    decode[Dbus.DevicePath](json) match
      case Right(devicePath) =>
        assertEqual(devicePath.toString, Dbus.DevicePath("/org/freedesktop/NetworkManager/Devices/3").toString)
      case Left(error) => fail(s"Failed to parse device path: $error")

  test("parse wireless device"):
    val json = file("wireless-device.json")
    decode[Dbus.WirelessDevice](json) match
      case Right(wirelessDevice) =>
        assertEqual(wirelessDevice.accessPoints.size, 17)
        assertEqual(
          wirelessDevice.accessPoints.head.toString,
          Dbus.AccessPointPath("/org/freedesktop/NetworkManager/AccessPoint/16214").toString
        )
        assertEqual(
          wirelessDevice.accessPoints.last.toString,
          Dbus.AccessPointPath("/org/freedesktop/NetworkManager/AccessPoint/16424").toString
        )
        assertEqual(
          wirelessDevice.active.toString,
          Dbus.AccessPointPath("/org/freedesktop/NetworkManager/AccessPoint/16395").toString
        )

      case Left(error) => fail(s"Failed to parse access point paths: $error")

  test("parse access point"):
    val json = file("access-point.json")
    decode[Dbus.AccessPoint](json) match
      case Right(accessPoint) =>
        assertEqual(accessPoint.ssid.toString, Dbus.Ssid("Ginkunai").toString)
        assertEqual(accessPoint.frequency.toString, Dbus.Frequency(5745).toString)
        assertEqual(accessPoint.mac.toString, Dbus.Mac("00:11:22:33:44:55").toString)
        assertEqual(accessPoint.strength.toString, Dbus.Strength(55).toString)
      case Left(error) => fail(s"Failed to parse access point: $error")
