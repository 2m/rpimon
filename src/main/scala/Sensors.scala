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

import java.lang.Long as JLong

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
      StateClass.Measurement,
      Units.Percent,
      active.ap.strength.unwrap.toLong
    ),
    Sensor(
      Id("wifi_freq"),
      Name("WiFi Channel Frequency"),
      Icon.Sine,
      StateClass.Measurement,
      Units.MHz,
      active.ap.frequency.unwrap.toLong
    ),
    Sensor(
      Id("wifi_bssid"),
      Name("WiFi BSSID"),
      Icon.Ap,
      StateClass.Measurement,
      Units.Meter, // need to set to some unit, so HA -> prometheus exporter picks it up
      active.ap.mac.toLong
    )
  )

given Sensors[Dbus.AccessPoint] with
  import HomeAssistant.*

  def mkSensors(ap: Dbus.AccessPoint)(using Dbus.System, Config) = List(
    Sensor(
      Id(s"ap_strength_${ap.mac.toLong}"),
      Name(s"AP ${ap.mac} Strength"),
      Icon.Wifi,
      StateClass.Measurement,
      Units.Percent,
      ap.strength.unwrap.toLong
    )
  )

extension (mac: Dbus.Mac) def toLong = mac.unwrap.replace(":", "").pipe(s => JLong.parseLong(s, 16))
