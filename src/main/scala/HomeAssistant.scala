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
    case Speedometer extends Icon("mdi:speedometer")
    case Thermometer extends Icon("mdi:thermometer")
    case Memory extends Icon("mdi:memory")
    case Calendar extends Icon("mdi:calendar")

  object Icon:
    given Encoder[Icon] = Encoder[String].contramap(_.value)

  enum State:
    case Measurement(units: Units, value: Int)
    case Binary(value: Boolean)

  extension (state: State)
    def config = state match
      case State.Measurement(units, _) =>
        Json.obj(
          "state_class" -> "measurement".asJson,
          "unit_of_measurement" -> units.asJson
        )
      case State.Binary(_) => Json.obj()
    def value = state match
      case State.Measurement(_, value) => value.toString
      case State.Binary(value)         => if value then "ON" else "OFF"
    def component = state match
      case State.Measurement(_, _) => "sensor"
      case State.Binary(_)         => "binary_sensor"

  enum Units(val value: String):
    case Percent extends Units("%")
    case MHz extends Units("MHz")
    case Meter extends Units("m")
    case Degrees extends Units("°C")
    case Days extends Units("days")

  object Units:
    given Encoder[Units] = Encoder[String].contramap(_.value)

  class Sensor(id: Id, name: Name, icon: Icon, state: State)(using sys: Dbus.System, conf: Config, hw: Stats.Hardware):
    def configValue = Json
      .obj(
        "unique_id" -> s"${sanitizedHostname}_$id".asJson,
        "object_id" -> s"${sanitizedHostname}_$id".asJson,
        "name" -> name.asJson,
        "icon" -> icon.asJson,
        "state_topic" -> stateTopic.asJson,
        "force_update" -> true.asJson,
        "expire_after" -> (5 * conf.tick.unwrap).toSeconds.asJson,
        "device" -> deviceConfig
      )
      .deepMerge(state.config)

    private def deviceConfig = Json.obj(
      "identifiers" -> Json.arr(sanitizedHostname.asJson),
      "manufacturer" -> BuildInfo.organizationName.asJson,
      "model" -> s"rpimon ${conf.version}".asJson,
      "name" -> sanitizedHostname.asJson,
      "sw_version" -> s"${sys.operatingSystem} / ${sys.kernelName} ${sys.kernelVersion}".asJson,
      "hw_version" -> hw.asJson,
      "configuration_url" -> BuildInfo.homepage.asJson
    )

    def configTopic = s"homeassistant/${state.component}/${conf.topicPrefix}/${sanitizedHostname}_$id/config"

    def stateValue = state.value
    def stateTopic = s"${conf.topicPrefix}/${sanitizedHostname}/$id"

    private def sanitizedHostname = sys.hostname.unwrap.replaceAll("[^a-zA-Z0-9-]", "_")
