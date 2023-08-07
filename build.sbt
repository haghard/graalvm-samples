name := "akka-graal-native"

organization := "com.github.haghard"

version := "0.1"

scalaVersion := "2.13.11"

val akkaVersion = "2.6.21"
val akkaHttpVersion = "10.2.10"
val log4j = "2.20.0"
val disruptor = "3.4.4"

Compile / scalacOptions ++= Seq(
  "-Xsource:3",
  "-language:experimental.macros",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Yrangepos",
  "-Xlog-reflective-calls",
  "-Xlint",
  "-Wconf:cat=other-match-analysis:error" //Transform exhaustive warnings into errors.
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  //log4j
  "org.apache.logging.log4j" % "log4j-api" % log4j,
  "org.apache.logging.log4j" % "log4j-core" % log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j,
  "com.lmax" % "disruptor" % disruptor,
)

enablePlugins(GraalVMNativeImagePlugin)

scalafmtOnCompile := true

addCommandAlias("c", "compile")
addCommandAlias("r", "reload")

graalVMNativeImageOptions ++= Seq(
  "--verbose",
  "--no-fallback",
  "--report-unsupported-elements-at-runtime",
  "-H:IncludeResources=.*\\.properties",
  "-H:ResourceConfigurationFiles=" + baseDirectory.value / "conf-agent" / "resource-config.json",
  "-H:ReflectionConfigurationFiles=" + baseDirectory.value / "conf-agent" / "reflect-config.json",
  "--enable-all-security-services",
  "--initialize-at-build-time",
  "--initialize-at-run-time=" +
    "akka.protobuf.DescriptorProtos," +
    "com.typesafe.config.impl.ConfigImpl$EnvVariablesHolder," +
    "com.oracle.truffle.js.scriptengine.GraalJSEngineFactory," +
    "com.typesafe.config.impl.ConfigImpl$SystemPropertiesHolder",
)

//graalvm-native-image:packageBin
// ./target/graalvm-native-image/akka-graal-native


//sbt assembly
//java -agentlib:native-image-agent=config-output-dir=conf-agent -jar ./target/scala-2.13/akka-graal-native-assembly-0.1.jar

//ab -n 1000 -c 3 http://localhost:8080/rnd

//https://youtu.be/T2MD9ULc-Io
//https://youtu.be/Pht0G2sqX4Q
//https://github.com/oracle/graalvm-reachability-metadata/tree/master/metadata
//https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/