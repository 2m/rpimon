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
import com.softwaremill.diffx.munit.DiffxAssertions
import fs2.Stream
import fs2.io.file.Path
import munit.CatsEffectSuite
import rpimon.Stats.*

object FileStatsSuite extends Util:
  val responses = Map(
    "/sys/firmware/devicetree/base/model" -> "Raspberry Pi 4 Model B Rev 1.5/",
    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq" -> "1500000",
    "/sys/class/thermal/thermal_zone0/temp" -> "63796",
    "/proc/loadavg" -> "1.65 1.91 1.95 1/490 28530",
    "/proc/cpuinfo" -> file("cpuinfo.txt"),
    "/proc/meminfo" -> file("meminfo.txt"),
    "/proc/uptime" -> "545832.69 1689235.32",
    "/proc/net/wireless" -> file("wireless.txt")
  )

  def mockFileSystem[F[_]](responses: Map[String, String]) = new FileSystem[F]:
    def readAll(file: Path): Stream[F, Byte] =
      responses
        .get(file.toString)
        .map(_.getBytes)
        .fold(throw new Error(s"No mocked file for [$file] found in ${responses.keys}"))(Stream.emits)

class FileStatsSuite extends CatsEffectSuite with DiffxAssertions with Util:
  import FileStatsSuite.*

  given FileSystem[IO] = mockFileSystem[IO](responses)
  val stats = FileStats[IO]

  test("get hardware"):
    stats.hardware().map(d => assertEqual(d.toString, Hardware("Raspberry Pi 4 Model B Rev 1.5/").toString))

  test("get cpu clock speed"):
    stats.cpuClockSpeed().map(d => assertEqual(d.toString, CpuClockSpeed(1500).toString))

  test("get cpu temperature"):
    stats.cpuTemperature().map(d => assertEqual(d.toString, CpuTemperature(64).toString))

  test("get cpu usage"):
    stats.cpuUsage().map(d => assertEqual(d.toString, CpuUsage(41).toString))

  test("get memory usage"):
    stats.memoryUsage().map(d => assertEqual(d.toString, MemoryUsage(19).toString))

  test("get uptime"):
    stats.uptime().map(d => assertEqual(d.toString, Uptime(6).toString))

  test("get wifi signal"):
    stats.wifiSignal().map(d => assertEqual(d.toString, WifiSignal(-64).toString))
