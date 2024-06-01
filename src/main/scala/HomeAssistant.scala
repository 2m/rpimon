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

import io.circe.*
import io.circe.syntax.*
import neotype.*
import neotype.interop.circe.given

object HomeAssistant:
  object Id extends Newtype[String]
  type Id = Id.Type

  object Name extends Newtype[String]
  type Name = Name.Type

  enum Icon(val value: String):
    case Wifi extends Icon("mdi:wifi")
    case Sine extends Icon("mdi:sine-wave")
    case Ap extends Icon("mdi:router-wireless")

  object Icon:
    given Encoder[Icon] = Encoder[String].contramap(_.value)

  enum StateClass:
    case Measurement

  object StateClass:
    given Encoder[StateClass] = Encoder[String].contramap(_.productPrefix.toLowerCase)

  enum Units(val value: String):
    case Percent extends Units("%")
    case MHz extends Units("MHz")
    case Meter extends Units("m")

  object Units:
    given Encoder[Units] = Encoder[String].contramap(_.value)

  class Sensor(id: Id, name: Name, icon: Icon, stateClass: StateClass, units: Units, value: Long)(using
      sys: Dbus.System,
      conf: Config
  ):
    def config = Json
      .obj(
        "unique_id" -> s"${sys.hostname}_$id".asJson,
        "name" -> name.asJson,
        "icon" -> icon.asJson,
        "state_class" -> stateClass.asJson,
        "unit_of_measurement" -> units.asJson,
        "state_topic" -> stateTopic.asJson,
        "device" -> deviceConfig
      )

    private def deviceConfig = Json.obj(
      "identifiers" -> Json.arr(sys.hostname.asJson),
      "manufacturer" -> BuildInfo.organizationName.asJson,
      "model" -> s"rpimon ${conf.version}".asJson,
      "name" -> sys.hostname.asJson,
      "sw_version" -> s"${sys.operatingSystem} / ${sys.kernelName} ${sys.kernelVersion}".asJson,
      "configuration_url" -> BuildInfo.homepage.asJson
    )

    def configTopic = s"homeassistant/sensor/${conf.topicPrefix}/${sys.hostname}_$id/config"

    def state = value.asJson
    def stateTopic = s"${conf.topicPrefix}/${sys.hostname}/$id"
