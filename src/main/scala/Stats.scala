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

import neotype.Newtype

object Stats:
  object Hardware extends Newtype[String]
  type Hardware = Hardware.Type

  object CpuClockSpeed extends Newtype[Int]
  type CpuClockSpeed = CpuClockSpeed.Type

  object CpuTemperature extends Newtype[Int]
  type CpuTemperature = CpuTemperature.Type

  object CpuUsage extends Newtype[Int]
  type CpuUsage = CpuUsage.Type

  object MemoryUsage extends Newtype[Int]
  type MemoryUsage = MemoryUsage.Type

  object Uptime extends Newtype[Int]
  type Uptime = Uptime.Type

trait Stats[F[_]]:
  import Stats.*

  def hardware(): F[Hardware]
  def cpuClockSpeed(): F[CpuClockSpeed]
  def cpuTemperature(): F[CpuTemperature]
  def cpuUsage(): F[CpuUsage]
  def memoryUsage(): F[MemoryUsage]
  def uptime(): F[Uptime]
