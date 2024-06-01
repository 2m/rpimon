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

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.legogroup.woof.DefaultLogger
import org.legogroup.woof.Logger

object RpiMonTest extends IOApp with Util:
  override def run(args: List[String]): IO[ExitCode] =
    given Proc[IO] = ProcessDbusSuite.mockProc[IO](ProcessDbusSuite.responses)
    for
      given Logger[IO] <- DefaultLogger.makeIo(consoleOutput)
      (publisher, sessionStatus) <- RpiMon.streams[IO]()
      _ <- IO.race(publisher.compile.drain, sessionStatus.compile.drain)
    yield ExitCode.Success
