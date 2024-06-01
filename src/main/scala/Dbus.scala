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

import io.circe.Decoder
import io.circe.HCursor
import neotype.*
import neotype.interop.circe.given

trait Wireless:
  object Device extends Newtype[String]
  type Device = Device.Type

  object DevicePath extends Newtype[String]
  type DevicePath = DevicePath.Type

  given Decoder[DevicePath] = new Decoder[DevicePath]:
    final def apply(c: HCursor): Decoder.Result[DevicePath] =
      for data <- c.downField("data").as[List[String]]
      yield DevicePath(data.head)

  object AccessPointPath extends Newtype[String]
  type AccessPointPath = AccessPointPath.Type

  case class WirelessDevice(active: AccessPointPath, accessPoints: List[AccessPointPath])

  given Decoder[WirelessDevice] = new Decoder[WirelessDevice]:
    final def apply(c: HCursor): Decoder.Result[WirelessDevice] =
      def data = c.downField("data").downArray
      for
        active <- data
          .downField("ActiveAccessPoint")
          .downField("data")
          .as[String]
          .map(AccessPointPath.apply)
        accessPoints <- data
          .downField("AccessPoints")
          .downField("data")
          .as[List[String]]
          .map(_.map(AccessPointPath.apply))
      yield WirelessDevice(active, accessPoints)

  object Ssid extends Newtype[String]
  type Ssid = Ssid.Type

  object Frequency extends Newtype[Int]
  type Frequency = Frequency.Type

  object Mac extends Newtype[String]
  type Mac = Mac.Type

  object Strength extends Newtype[Int]
  type Strength = Strength.Type

  case class AccessPoint(ssid: Ssid, frequency: Frequency, mac: Mac, strength: Strength)
  case class ActiveAccessPoint(ap: AccessPoint)

  given Decoder[AccessPoint] = new Decoder[AccessPoint]:
    final def apply(c: HCursor): Decoder.Result[AccessPoint] =
      def data = c.downField("data").downArray
      for
        ssid <- data.downField("Ssid").downField("data").as[Array[Byte]].map(bytes => Ssid(String(bytes)))
        frequency <- data.downField("Frequency").downField("data").as[Frequency]
        mac <- data.downField("HwAddress").downField("data").as[Mac]
        strength <- data.downField("Strength").downField("data").as[Strength]
      yield AccessPoint(ssid, frequency, mac, strength)

trait System:
  object Hostname extends Newtype[String]
  type Hostname = Hostname.Type

  object KernelName extends Newtype[String]
  type KernelName = KernelName.Type

  object KernelVersion extends Newtype[String]
  type KernelVersion = KernelVersion.Type

  object OperatingSystem extends Newtype[String]
  type OperatingSystem = OperatingSystem.Type

  case class System(
      hostname: Hostname,
      kernelName: KernelName,
      kernelVersion: KernelVersion,
      operatingSystem: OperatingSystem
  )

  given Decoder[System] = new Decoder[System]:
    final def apply(c: HCursor): Decoder.Result[System] =
      def data = c.downField("data").downArray
      for
        hostname <- data.downField("Hostname").downField("data").as[Hostname]
        kernelName <- data.downField("KernelName").downField("data").as[KernelName]
        kernelVersion <- data.downField("KernelRelease").downField("data").as[KernelVersion]
        operatingSystem <- data.downField("OperatingSystemPrettyName").downField("data").as[OperatingSystem]
      yield System(hostname, kernelName, kernelVersion, operatingSystem)

object Dbus extends Wireless with System

trait Dbus[F[_]]:
  import Dbus.*

  def devicePath(device: Device): F[Dbus.DevicePath]
  def wirelessDevice(devicePath: DevicePath): F[Dbus.WirelessDevice]
  def accessPoint(accessPointPath: Dbus.AccessPointPath): F[Dbus.AccessPoint]
  def system(): F[Dbus.System]
