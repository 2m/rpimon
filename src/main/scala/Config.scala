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

import scala.concurrent.duration.*

import Config.*
import cats.syntax.all.*
import ciris.*
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import neotype.Newtype

case class Config(
    tick: Tick,
    topicPrefix: TopicPrefix,
    mqttHost: MqttHost,
    mqttPort: MqttPort,
    wirelessDevice: WirelessDevice,
    macFriendlyNames: MacFriendlyNames,
    version: Version
)

object Config:
  object Tick extends Newtype[FiniteDuration]:
    override inline def validate(input: FiniteDuration) =
      if input > 0.seconds then true else "Tick duration must be larger than zero"
  type Tick = Tick.Type

  object TopicPrefix extends Newtype[String]:
    override inline def validate(input: String) =
      if input.nonEmpty then true else "Topic prefix must not be empty"
  type TopicPrefix = TopicPrefix.Type

  object MqttHost extends Newtype[Host]
  type MqttHost = MqttHost.Type

  object MqttPort extends Newtype[Port]
  type MqttPort = MqttPort.Type

  object WirelessDevice extends Newtype[String]:
    override inline def validate(input: String) =
      if input.nonEmpty then true else "Wireless device must not be empty"
  type WirelessDevice = WirelessDevice.Type

  object MacFriendlyNames extends Newtype[Map[String, String]]
  type MacFriendlyNames = MacFriendlyNames.Type

  object Version extends Newtype[String]
  type Version = Version.Type

val config =
  (
    env("RPIMON_TICK").default(5.seconds.toString).as[Tick],
    env("RPIMON_TOPIC_PREFIX").default("rpimon").as[TopicPrefix],
    env("RPIMON_MQTT_HOST").default("localhost").as[MqttHost],
    env("RPIMON_MQTT_PORT").default("1883").as[MqttPort],
    env("RPIMON_WIRELESS_DEVICE").default("wlan0").as[WirelessDevice],
    env("RPIMON_MAC_FRIENDLY_NAMES").default("").as[MacFriendlyNames],
    default(BuildInfo.version).as[Version]
  ).parMapN(Config.apply)
