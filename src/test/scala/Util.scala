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

import java.nio.file.Files

import scala.concurrent.duration.*

import com.comcast.ip4s.*
import com.indoorvivants.snapshots.munit_integration.MunitSnapshotsIntegration

trait Util:
  import Config.*

  def file(filename: String) =
    Files.readString(BuildInfo.test_resourceDirectory.toPath().resolve(filename + ".json"))

  given Config =
    Config(
      Tick.unsafeMake(2.seconds),
      TopicPrefix("rpimon"),
      MqttHost(host"localhost"),
      MqttPort(port"1883"),
      WirelessDevice("wlan0"),
      Version("0.0.0")
    )

trait SnapshotAssertions extends MunitSnapshotsIntegration:
  this: munit.FunSuite =>

  val snapshot = FunFixture[(String) => Unit](
    setup = test => (value: String) => assertSnapshot(test.name, value),
    teardown = _ => ()
  )
