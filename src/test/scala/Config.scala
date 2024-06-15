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

import cats.effect.IO
import ciris.*
import munit.CatsEffectSuite
import neotype.*

class ConfigSuite extends CatsEffectSuite:
  import Config.*

  test("parse mac friendly names"):
    default("3C:28:6D:6C:F9:00=Po Virtuve 5GHz")
      .as[MacFriendlyNames]
      .attempt[IO]
      .map:
        case Right(value) => assertEquals(value.unwrap, Map("3C:28:6D:6C:F9:00" -> "Po Virtuve 5GHz"))
        case Left(error)  => fail(s"Failed to parse mac friendly names: $error")

  test("skip white space"):
    default("3C:28:6D:6C:F9:00=Po Virtuve 5GHz,3C:28:6D:6C:F9:04=Po Virtuve 2.4GHz")
      .as[MacFriendlyNames]
      .attempt[IO]
      .map:
        case Right(value) =>
          assertEquals(
            value.unwrap,
            Map("3C:28:6D:6C:F9:00" -> "Po Virtuve 5GHz", "3C:28:6D:6C:F9:04" -> "Po Virtuve 2.4GHz")
          )
        case Left(error) => fail(s"Failed to parse mac friendly names: $error")

  test("parse empty"):
    default("")
      .as[MacFriendlyNames]
      .attempt[IO]
      .map:
        case Right(value) => assertEquals(value.unwrap, Map.empty)
        case Left(error)  => fail(s"Failed to parse mac friendly names: $error")
