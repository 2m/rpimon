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

import cats.Applicative
import cats.syntax.all.*
import fs2.Stream
import neotype.*
import org.legogroup.woof.{*, given}

case class ApStream[F[_]](active: Stream[F, Dbus.ActiveAccessPoint], accessPoints: Stream[F, Dbus.AccessPoint])

def apStream[F[_]: Logger: Applicative]()(using dbus: Dbus[F], conf: Config) =
  val stream = for
    devicePath <- Stream.eval(dbus.devicePath(Dbus.Device(conf.wirelessDevice.unwrap)))
    wirelessDevice <- Stream.eval(dbus.wirelessDevice(devicePath))
    activeAccessPoint <- getAccessPoint(wirelessDevice.active).map(Dbus.ActiveAccessPoint.apply)
    accessPoints = Stream.emits(wirelessDevice.accessPoints).flatMap(getAccessPoint)
  yield ApStream(Stream(activeAccessPoint), accessPoints.filter(_.ssid == activeAccessPoint.ap.ssid))

  stream.handleErrorWith(err =>
    Stream.eval(
      Logger[F].info(s"Unable to get AP info (${err.getMessage})") *>
        ApStream(Stream.empty, Stream.empty).pure[F]
    )
  )

def getAccessPoint[F[_]: Logger](path: Dbus.AccessPointPath)(using dbus: Dbus[F]) =
  Stream
    .eval(dbus.accessPoint(path))
    .handleErrorWith(_ => Stream.eval(Logger[F].info(s"Skipping AP [$path] as it does not exist anymore")).drain)

def statsStream[F[_]: Logger]()(using stats: Stats[F], sys: Dbus.System, hw: Stats.Hardware, conf: Config) =
  mkSensorOrError(stats.cpuClockSpeed()) ++
    mkSensorOrError(stats.cpuTemperature()) ++
    mkSensorOrError(stats.cpuUsage()) ++
    mkSensorOrError(stats.memoryUsage()) ++
    mkSensorOrError(stats.uptime()) ++
    mkSensorOrError(stats.wifiSignal())

def sensorStream[F[_]: Logger: Applicative]()(using dbus: Dbus[F], stats: Stats[F], config: Config) =
  for
    given Dbus.System <- Stream.eval(dbus.system())
    given Stats.Hardware <- Stream.eval(stats.hardware())
    ApStream(active, accessPoints) <- apStream()
    sensor <- Stream.empty
      .append(active.map(Sensors.mkSensors))
      .append(accessPoints.map(Sensors.mkSensors))
      .append(statsStream())
      .flatMap(Stream.emits)
  yield sensor

private def mkSensorOrError[F[_]: Logger, T](t: F[T])(using Sensors[T], Dbus.System, Stats.Hardware, Config) =
  Stream
    .eval(t)
    .handleErrorWith(t => Stream.eval(Logger[F].info(t.getMessage)).drain)
    .map(Sensors.mkSensors)
