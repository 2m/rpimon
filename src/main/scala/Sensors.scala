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

import scala.util.chaining.*

import neotype.*

trait Sensors[T]:
  def mkSensors(t: T)(using Dbus.System, Config): List[HomeAssistant.Sensor]

object Sensors:
  def mkSensors[T](t: T)(using Sensors[T], Dbus.System, Config) =
    summon[Sensors[T]].mkSensors(t)

given Sensors[Dbus.ActiveAccessPoint] with
  import HomeAssistant.*

  def mkSensors(active: Dbus.ActiveAccessPoint)(using Dbus.System, Config) = List(
    Sensor(
      Id("wifi_strength"),
      Name("WiFi Strength"),
      Icon.Wifi,
      State.Measurement(Units.Percent, active.ap.strength.unwrap)
    ),
    Sensor(
      Id("wifi_freq"),
      Name("WiFi Channel Frequency"),
      Icon.Sine,
      State.Measurement(Units.MHz, active.ap.frequency.unwrap)
    ),
    Sensor(
      Id("wifi_bssid"),
      // we want to export this sensor from HA to prometheus,
      // but HA only exports integer sensor values. We set name to bssid
      // so the value is exported as `friendly_name` label in prometheus.
      Name(active.ap.mac.unwrap),
      Icon.Ap,
      State.Binary(true)
    )
  )

given Sensors[Dbus.AccessPoint] with
  import HomeAssistant.*

  def mkSensors(ap: Dbus.AccessPoint)(using Dbus.System, Config) = List(
    Sensor(
      Id(s"ap_strength_${ap.mac.toHex}"),
      Name(s"AP ${ap.mac} Strength"),
      Icon.Wifi,
      State.Measurement(Units.Percent, ap.strength.unwrap)
    )
  )

extension (mac: Dbus.Mac) def toHex = mac.unwrap.replace(":", "")
