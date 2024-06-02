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

import io.circe.syntax.*

class HomeAssistantSuite extends munit.FunSuite with SnapshotAssertions with Util:
  import Config.*
  import Dbus.*
  import HomeAssistant.*

  given System = Dbus.System(
    Hostname("openmower"),
    KernelName("Linux"),
    KernelVersion("6.1.21-v8+"),
    OperatingSystem("Debian GNU/Linux 11 (bullseye)")
  )

  def sensor(using Dbus.System) = Sensor(
    Id("wifi_bssid"),
    Name("WiFi BSSID"),
    Icon.Ap,
    State.Measurement(Units.Meter, 123)
  )

  def binarySensor(using Dbus.System) = Sensor(
    Id("wifi_on"),
    Name("WiFi On"),
    Icon.Ap,
    State.Binary(true)
  )

  snapshot.test("sensor config"): assertSnapshot =>
    assertSnapshot(sensor.configValue.spaces4)

  snapshot.test("binary sensor config"): assertSnapshot =>
    assertSnapshot(binarySensor.configValue.spaces4)

  test("sensor config topic"):
    assertEquals(sensor.configTopic, "homeassistant/sensor/rpimon/openmower_wifi_bssid/config")
    assertEquals(binarySensor.configTopic, "homeassistant/binary_sensor/rpimon/openmower_wifi_on/config")

  test("sensor state"):
    assertEquals(sensor.stateValue, 123.asJson)
    assertEquals(binarySensor.stateValue, "ON".asJson)

  test("sensor state topic"):
    assertEquals(sensor.stateTopic, "rpimon/openmower/wifi_bssid")
    assertEquals(binarySensor.stateTopic, "rpimon/openmower/wifi_on")

  test("sensor object_id generated from id"):
    val sensor = Sensor(
      Id("wifi_freq"),
      Name("WiFi Channel Frequency"),
      Icon.Sine,
      State.Measurement(Units.MHz, 123)
    )
    assertEquals(sensor.configValue.hcursor.downField("object_id").as[String], Right("wifi_freq"))

  test("sanitized hostname"):
    given System = Dbus.System(
      Hostname("openmower.local"),
      KernelName("Linux"),
      KernelVersion("6.1.21-v8+"),
      OperatingSystem("Debian GNU/Linux 11 (bullseye)")
    )
    assertEquals(sensor.configValue.hcursor.downField("unique_id").as[String], Right("openmower_local_wifi_bssid"))
