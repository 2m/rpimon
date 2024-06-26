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

import scala.language.future

class SensorsSuite extends munit.FunSuite with SnapshotAssertions with Util:
  import Config.*
  import Dbus.*
  import Stats.*

  given System = Dbus.System(
    Hostname("openmower"),
    KernelName("Linux"),
    KernelVersion("6.1.21-v8+"),
    OperatingSystem("Debian GNU/Linux 11 (bullseye)")
  )

  given Hardware = Stats.Hardware("RPi 4 Model B Rev 1.2")

  val ap = AccessPoint(
    Ssid("Ginkunai"),
    Frequency(5745),
    Mac("00:11:22:33:44:55"),
    Strength(55)
  )
  val activeAp = ActiveAccessPoint(ap)

  test("active ap sensors"):
    val s = summon[Sensors[ActiveAccessPoint]]
    val sensors = s.mkSensors(activeAp)
    assertEquals(sensors.size, 3)
    assertEquals(sensors.map(_.stateValue.toString), List("55", "5745", "ON"))

  snapshot.test("ap sensors"): assertSnapshot =>
    val s = summon[Sensors[AccessPoint]]
    val sensors = s.mkSensors(ap)
    assertEquals(sensors.size, 1)
    assertSnapshot(sensors.head.configValue.spaces4)

  snapshot.test("active ap strength sensor with friendly name"): assertSnapshot =>
    given Config =
      summon[Config].copy(macFriendlyNames = MacFriendlyNames(Map("00:11:22:33:44:55" -> "Friendly AP Name")))
    val s = summon[Sensors[ActiveAccessPoint]]
    val sensors = s.mkSensors(activeAp)
    assertEquals(sensors.size, 3)
    assertSnapshot(sensors.find(_.stateTopic.contains("wifi_bssid")).get.configValue.spaces4)

  snapshot.test("ap sensors with friendly name"): assertSnapshot =>
    given Config =
      summon[Config].copy(macFriendlyNames = MacFriendlyNames(Map("00:11:22:33:44:55" -> "Friendly AP Name")))
    val s = summon[Sensors[AccessPoint]]
    val sensors = s.mkSensors(ap)
    assertEquals(sensors.size, 1)
    assertSnapshot(sensors.head.configValue.spaces4)
