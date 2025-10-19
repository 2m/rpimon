scalaVersion := "3.7.0"
scalafmtOnCompile := true

libraryDependencies ++= Seq(
  "co.fs2"                 %% "fs2-io"            % "3.12.2",
  "io.circe"               %% "circe-core"        % "0.14.15",
  "io.circe"               %% "circe-parser"      % "0.14.15",
  "io.github.kitlangton"   %% "neotype"           % "0.3.25",
  "io.github.kitlangton"   %% "neotype-circe"     % "0.3.25",
  "net.sigusr"             %% "fs2-mqtt"          % "1.0.1",
  "is.cir"                 %% "ciris"             % "3.11.0",
  "org.legogroup"          %% "woof-core"         % "0.7.0",
  "org.scalameta"          %% "munit"             % "1.2.1" % Test,
  "org.typelevel"          %% "munit-cats-effect" % "2.1.0" % Test,
  "com.softwaremill.diffx" %% "diffx-munit"       % "0.9.0" % Test,
  compilerPlugin("com.github.ghik" % "zerowaste" % "1.0.0" cross CrossVersion.full)
)

Compile / run / fork := true

enablePlugins(BuildInfoPlugin)
buildInfoKeys := Seq[BuildInfoKey](
  version,
  organizationName,
  BuildInfoKey.map(homepage) { case (k, v) => k -> v.fold("")(_.toString) },
  Test / resourceDirectory
)
buildInfoPackage := "rpimon"

assembly / assemblyOutputPath := target.value / "rpimon.jar"

enablePlugins(AutomateHeaderPlugin)
organization := "rpimon"
organizationName := "github.com/2m/rpimon/contributors"
startYear := Some(2024)
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage := Some(url("https://github.com/2m/rpimon"))

enablePlugins(SnapshotsPlugin)
snapshotsPackageName := buildInfoPackage.value
snapshotsIntegrations += SnapshotIntegration.MUnit
