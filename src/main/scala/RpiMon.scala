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
import fs2.Stream
import neotype.*
import org.legogroup.woof.{*, given}

case class ApStream[F[_]](active: Stream[F, Dbus.ActiveAccessPoint], accessPoints: Stream[F, Dbus.AccessPoint])

def apStream[F[_]: Logger]()(using dbus: Dbus[F], conf: Config) =
  for
    devicePath <- Stream.eval(dbus.devicePath(Dbus.Device(conf.wirelessDevice.unwrap)))
    wirelessDevice <- Stream.eval(dbus.wirelessDevice(devicePath))
    activeAccessPoint = Stream.emit(wirelessDevice.active).flatMap(getAccessPoint).map(Dbus.ActiveAccessPoint.apply)
    accessPoints = Stream.emits(wirelessDevice.accessPoints).flatMap(getAccessPoint)
  yield ApStream(activeAccessPoint, accessPoints)

def getAccessPoint[F[_]: Logger](path: Dbus.AccessPointPath)(using dbus: Dbus[F]) =
  Stream
    .eval(dbus.accessPoint(path))
    .handleErrorWith(_ => Stream.eval(Logger[F].info(s"Skipping $path as it does not exist anymore")).drain)

def sensorStream[F[_]: Logger]()(using dbus: Dbus[F], config: Config) =
  for
    system <- Stream.eval(dbus.system())
    given Dbus.System = system
    ApStream(active, accessPoints) <- apStream()
    sensor <-
      active
        .map(Sensors.mkSensors)
        .append(accessPoints.map(Sensors.mkSensors))
        .flatMap(Stream.emits)
  yield sensor
