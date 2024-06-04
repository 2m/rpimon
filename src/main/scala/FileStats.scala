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

import cats.effect.Concurrent
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.Files
import fs2.io.file.Path
import fs2.text

trait FileSystem[F[_]]:
  def readAll(file: Path): Stream[F, Byte]

object FileSystem:
  def readAll[F[_]: FileSystem](file: Path) = summon[FileSystem[F]].readAll(file)

class Fs2FileSystem[F[_]: Files] extends FileSystem[F]:
  def readAll(file: Path): Stream[F, Byte] =
    Files[F].readAll(file)

class FileStats[F[_]: FileSystem: Concurrent] extends Stats[F]:
  import Stats.*

  def hardware() =
    FileSystem
      .readAll[F](Path("/sys/firmware/devicetree/base/model"))
      .through(text.utf8.decode)
      .compile
      .string
      .map(Hardware.apply)

  def cpuClockSpeed() =
    FileSystem
      .readAll[F](Path("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"))
      .through(text.utf8.decode)
      .compile
      .string
      .map(_.trim.toInt / 1000)
      .map(CpuClockSpeed.apply)

  def cpuTemperature() =
    FileSystem
      .readAll[F](Path("/sys/class/thermal/thermal_zone0/temp"))
      .through(text.utf8.decode)
      .compile
      .string
      .map(t => (t.trim.toInt / 1000.0).round.toInt)
      .map(CpuTemperature.apply)

  def cpuUsage() =
    for
      loadAvg1minute <- FileSystem
        .readAll[F](Path("/proc/loadavg"))
        .through(text.utf8.decode)
        .compile
        .string
        .map(_.split(" ").head.pipe(BigDecimal.apply))
      cpuCount <- FileSystem
        .readAll[F](Path("/proc/cpuinfo"))
        .through(text.utf8.decode)
        .through(text.lines)
        .filter(_.startsWith("processor"))
        .compile
        .count
    yield CpuUsage((loadAvg1minute * 100 / cpuCount).toInt)

  def memoryUsage() =
    for
      meminfo <- FileSystem
        .readAll[F](Path("/proc/meminfo"))
        .through(text.utf8.decode)
        .through(text.lines)
        .compile
        .toList
      memTotal <- meminfo
        .find(_.startsWith("MemTotal"))
        .map(_.split(" ").init.last.toInt)
        .get
        .pure[F]
      memAvailable <- meminfo
        .find(_.startsWith("MemAvailable"))
        .map(_.split(" ").init.last.toInt)
        .get
        .pure[F]
    yield MemoryUsage(((memTotal - memAvailable) * 100 / memTotal).toInt)

  def uptime() =
    FileSystem
      .readAll[F](Path("/proc/uptime"))
      .through(text.utf8.decode)
      .compile
      .string
      .map(_.split(" ").head.pipe(BigDecimal.apply).toInt / 3600 / 24)
      .map(Uptime.apply)

  def wifiSignal() =
    FileSystem
      .readAll[F](Path("/proc/net/wireless"))
      .through(text.utf8.decode)
      .through(text.lines)
      .drop(2)
      .head
      .map(_.trim.split(" +").drop(3).head.replace(".", "").toInt)
      .map(WifiSignal.apply)
      .compile
      .onlyOrError
