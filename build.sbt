scalaVersion := "3.3.3"

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
vcpkgDependencies := VcpkgDependencies("paho-mqtt")
//vcpkgNativeConfig ~= { _.addRenamedLibrary("paho-mqtt", "libpaho-mqtt3a") }
bindgenBindings := Seq(
  Binding(vcpkgConfigurator.value.includes("paho-mqtt") / "MQTTClient.h", "libpahomqtt")
    .withCImports(List("MQTTClient.h"))
)
