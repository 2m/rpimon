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

import cats.MonadError
import cats.effect.Concurrent
import cats.effect.kernel.Resource
import cats.syntax.all.*
import fs2.Stream
import fs2.io.process.{ProcessBuilder, Processes}
import fs2.text
import io.circe.parser.*

trait Proc[F[_]]:
  def spawn(command: String, args: String*): Resource[F, Stream[F, Byte]]

object Proc:
  def run[F[_]: Proc](command: String, args: String*) = summon[Proc[F]].spawn(command, args*)

class Fs2Proc[F[_]: Processes] extends Proc[F]:
  def spawn(command: String, args: String*): Resource[F, Stream[F, Byte]] =
    ProcessBuilder(command, args*).spawn.map(_.stdout)

class ProcessDbus[F[_]: Proc: Concurrent] extends Dbus[F]:
  import Dbus.*

  def devicePath(device: Device) =
    Proc
      .run(
        "busctl",
        "-j",
        "call",
        "org.freedesktop.NetworkManager",
        "/org/freedesktop/NetworkManager",
        "org.freedesktop.NetworkManager",
        "GetDeviceByIpIface",
        "s",
        device.toString
      )
      .use: p =>
        p.through(text.utf8.decode)
          .compile
          .string
          .map(json => decode[DevicePath](json))
          .flatMap:
            case Right(value) => value.pure[F]
            case Left(error)  => summon[MonadError[F, Throwable]].raiseError(error)

  def wirelessDevice(devicePath: DevicePath) =
    Proc
      .run(
        "busctl",
        "-j",
        "call",
        "org.freedesktop.NetworkManager",
        devicePath.toString,
        "org.freedesktop.DBus.Properties",
        "GetAll",
        "s",
        "org.freedesktop.NetworkManager.Device.Wireless"
      )
      .use: p =>
        p.through(text.utf8.decode)
          .compile
          .string
          .map(json => decode[Dbus.WirelessDevice](json))
          .flatMap:
            case Right(value) => value.pure[F]
            case Left(error)  => summon[MonadError[F, Throwable]].raiseError(error)

  def accessPoint(accessPointPath: Dbus.AccessPointPath) =
    Proc
      .run(
        "busctl",
        "-j",
        "call",
        "org.freedesktop.NetworkManager",
        accessPointPath.toString,
        "org.freedesktop.DBus.Properties",
        "GetAll",
        "s",
        "org.freedesktop.NetworkManager.AccessPoint"
      )
      .use: p =>
        p.through(text.utf8.decode)
          .compile
          .string
          .map(json => decode[Dbus.AccessPoint](json))
          .flatMap:
            case Right(value) => value.pure[F]
            case Left(error)  => summon[MonadError[F, Throwable]].raiseError(error)

  def system() =
    Proc
      .run(
        "busctl",
        "-j",
        "call",
        "org.freedesktop.hostname1",
        "/org/freedesktop/hostname1",
        "org.freedesktop.DBus.Properties",
        "GetAll",
        "s",
        "org.freedesktop.hostname1"
      )
      .use: p =>
        p.through(text.utf8.decode)
          .compile
          .string
          .map(json => decode[Dbus.System](json))
          .flatMap:
            case Right(value) => value.pure[F]
            case Left(error)  => summon[MonadError[F, Throwable]].raiseError(error)
