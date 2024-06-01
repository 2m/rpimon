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

import com.indoorvivants.snapshots.munit_integration.*
import io.circe.syntax.*

class HomeAssistantSuite extends munit.FunSuite with MunitSnapshotsIntegration with Util:
  import Config.*
  import Dbus.*
  import HomeAssistant.*

  given System = Dbus.System(
    Hostname("openmower"),
    KernelName("Linux"),
    KernelVersion("6.1.21-v8+"),
    OperatingSystem("Debian GNU/Linux 11 (bullseye)")
  )

  val sensor = Sensor(
    id = Id("wifi_bssid"),
    name = Name("WiFi BSSID"),
    icon = Icon.Ap,
    stateClass = StateClass.Measurement,
    units = Units.Meter,
    value = 123L
  )

  test("sensor config"):
    assertSnapshot("config", sensor.config.spaces4)

  test("sensor config topic"):
    assertEquals(sensor.configTopic, "homeassistant/sensor/rpimon/openmower_wifi_bssid/config")

  test("sensor state"):
    assertEquals(sensor.state, 123.asJson)

  test("sensor state topic"):
    assertEquals(sensor.stateTopic, "rpimon/openmower/wifi_bssid")
