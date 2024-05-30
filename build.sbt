scalaVersion := "3.3.3"

libraryDependencies += "ch.epfl.lamp" %%% "gears" % "0.2.0"

enablePlugins(ScalaNativePlugin, BindgenPlugin, VcpkgNativePlugin)

import scala.scalanative.build.*
nativeConfig ~= { c =>
  c.withLTO(LTO.none) // thin
    .withMode(Mode.debug) // releaseFast
    .withGC(GC.immix) // commix
}

import com.indoorvivants.detective.Platform
import com.indoorvivants.detective.Platform.OS.*
nativeConfig := {
  val conf = nativeConfig.value
  val arch64 =
    if (Platform.arch == Platform.Arch.Arm && Platform.bits == Platform.Bits.x64)
      List("-arch", "arm64")
    else Nil

  conf
    .withLinkingOptions(
      conf.linkingOptions ++ arch64
    )
    .withCompileOptions(
      conf.compileOptions ++ arch64
    )
}

import bindgen.interface.Binding
bindgenBindings := Seq(
  Binding(baseDirectory.value / "mqttc-amalgam.h", "libmqttc")
    .withCImports(List("mqtt.h", "mqtt_pal.h"))
    .withClangFlags(
      List(
        "-I" + baseDirectory.value / "mqtt-c" / "include"
      )
    )
)
