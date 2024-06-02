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

import scala.concurrent.duration.*

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Async
import cats.effect.kernel.Sync
import cats.effect.kernel.Temporal
import cats.effect.std.Console
import cats.syntax.all.*
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import fs2.Stream
import fs2.io.net.Network
import fs2.io.process.Processes
import neotype.*
import net.sigusr.mqtt.api.*
import net.sigusr.mqtt.api.ConnectionState.*
import org.legogroup.woof.{*, given}

object RpiMon extends IOApp:

  private def ticks[F[_]: Temporal](using conf: Config): Stream[F, Unit] =
    Stream.sleep[F](conf.tick.unwrap).repeat

  private def onConnectionState[F[_]: Sync: Logger]: ConnectionState => F[Unit] = {
    case Error(e) => Sync[F].raiseError(e)
    case Connecting(nextDelay, retriesSoFar) =>
      Logger[F].info(s"Connecting to MQTT broker, next delay: ${nextDelay.toCoarsest}, retries so far: $retriesSoFar")
    case Connected => Logger[F].info("Connected to MQTT broker")
    case _         => Sync[F].pure(())
  }

  def streams[F[_]: Processes: Logger: Console: Network: Async: Proc]()(using conf: Config) =
    val transportConfig = TransportConfig[F](conf.mqttHost.unwrap, conf.mqttPort.unwrap)
    val sessionConfig = SessionConfig("rpimon")

    given Dbus[F] = ProcessDbus[F]
    val sensors = sensorStream[F]()

    Session[F](transportConfig, sessionConfig).use: session =>
      val sessionStatus = session.state.discrete.evalMap(onConnectionState[F])

      val publisher = ticks
        .flatMap(_ => sensors)
        .evalMap: s =>
          session.publish(s.configTopic, s.configValue.noSpaces.getBytes("UTF-8").toVector) *>
            session.publish(s.stateTopic, s.stateValue.getBytes("UTF-8").toVector)

      Logger[F].info(s"rpimon will report every ${conf.tick} to ${conf.mqttHost}:${conf.mqttPort}") *>
        (publisher, sessionStatus).pure[F]

  override def run(args: List[String]): IO[ExitCode] =
    given Proc[IO] = Fs2Proc[IO]
    for
      given Logger[IO] <- DefaultLogger.makeIo(consoleOutput)
      given Config <- config.load[IO]
      (publisher, sessionStatus) <- streams[IO]()
      _ <- IO.race(publisher.compile.drain, sessionStatus.compile.drain)
    yield ExitCode.Success
