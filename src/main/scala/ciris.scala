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

import cats.syntax.all.*
import ciris.ConfigDecoder
import ciris.ConfigError
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import neotype.Newtype

given [A, B](using newType: Newtype.WithType[A, B], decoder: ConfigDecoder[String, A]): ConfigDecoder[String, B] =
  decoder.mapEither:
    case (key, value) => newType.make(value).left.map(err => ConfigError(err))

given ConfigDecoder[String, Host] =
  ConfigDecoder[String, String].mapOption("hostname")(Host.fromString(_))

given ConfigDecoder[String, Port] =
  ConfigDecoder[String, String].mapOption("port")(Port.fromString(_))

given ConfigDecoder[String, Map[String, String]] =
  ConfigDecoder[String, String].mapEither:
    case (_, "") => Right(Map.empty)
    case (_, input) =>
      input
        .split(",")
        .toList
        .map: entry =>
          entry.split("=").toList match
            case mac :: friendlyName :: Nil => Right(mac -> friendlyName)
            case _                          => Left(ConfigError(s"Unable to find values separated by '=' in [$entry]"))
        .sequence
        .map(_.toMap)
