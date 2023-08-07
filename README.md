# Akka HTTP + GraalVM native
Example project with simple Akka HTTP server compiled with GraalVM native-image.

  
### Compiling
    
    sbt graalvm-native-image:packageBin
    
It might take a few minutes to compile.
   
### Running
    
    # MacOS:
    ./target/graalvm-native-image/akka-graal-native
    
    And then we can use ab to test it: 
    ab -n 1000 -c 3 http://localhost:8080/rnd    

### To generate conf-agent folder

    1. sbt assembly
    2. java -agentlib:native-image-agent=config-output-dir=conf-agent -jar ./target/scala-2.13/akka-graal-native-assembly-0.1.jar
    3. In a separate window run 
       ab -n 10000 -c 3 http://localhost:8080/rnd
    4. Ctrl+C to exit the running process.


## How it works
Most of the Akka-specific configuration for `native-image` is provided by [akka-graal-config](https://github.com/vmencik/akka-graal-config)
repository which publishes a set of jar artifacts that contain the necessary configuration resources
for `native-image` to compile Akka modules. Just having these jars in the classpath is enough
for `native-image` to pick up this configuration.
See [this blog post](https://medium.com/graalvm/simplifying-native-image-generation-with-maven-plugin-and-embeddable-configuration-d5b283b92f57)
for more details on how that mechanism works.

### Reflection configuration
See [SubstrateVM docs](https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md)
for details.

Configuration for Akka itself is provided by [akka-graal-config](https://github.com/vmencik/akka-graal-config)
dependencies. This repo contains only reflection configuration to get java.util.logging working.

Note however that reflective access to `context` and `self` fields must be configured for every actor
that is monitored with `context.watch` (observed empirically).
Otherwise, you'll get [an error from Akka's machinery](https://github.com/akka/akka/blob/v2.5.21/akka-actor/src/main/scala/akka/actor/ActorCell.scala#L711).

### Affinity Pool
`akka.dispatch.affinity.AffinityPool` is using MethodHandles which causes errors like this one
during native image build:

    Error: com.oracle.graal.pointsto.constraints.UnsupportedFeatureException: Invoke with MethodHandle argument could not be reduced to at most a single call: java.lang.invoke.MethodHandle.bindTo(Object)
    
The workaround is to initialize affected classes (and we actually do this for the whole classpath)
at build time using the `--initalize-at-build-time` option.

### Lightbend Config
Static initializers of `com.typesafe.config.impl.ConfigImpl$EnvVariablesHolder`
and `com.typesafe.config.impl.ConfigImpl$SystemPropertiesHolder` need to be run at runtime using
the `--initialize-at-run-time` option.
Otherwise the environment from image build time will be baked in to the configuration.

### Akka Scheduler and sun.misc.Unsafe
To make the default Akka scheduler work with SubstrateVM it is necessary to recalculate the field
offset that it uses with sun.misc.Unsafe.
This is done by using SubstrateVM API in `AkkaSubstitutions` class from `graal-akka-actor` dependency.

For more details see the section about Unsafe in this [blog post](https://medium.com/graalvm/instant-netty-startup-using-graalvm-native-image-generation-ed6f14ff7692).

Note that this substitution is only necessary with Scala 2.12. Curiously with Scala 2.13 `native-image`
can make the substitution itself automatically. Probably due to some difference in the emitted
bytecode.

### MethodHandle in scala.runtime.Statics
In Scala 2.13 a MethodHandle is used in `Statics.releaseFence()` to invoke either
`java.lang.invoke.VarHandle.releaseFence()` if running in Java 9 VM or `sun.misc.Unsafe.storeFence()`
if on Java 8. As noted above, MethodHandles are a problem with `native-image` but since GraalVM is
currently based on Java 8 `Statics.releaseFence()` can be substituted to always call Unsafe without
using MethodHandle. This is done by `ScalaSubstitutions` present in Scala 2.13 version of
`graal-akka-actor`.

### Serialization
SubstrateVM does not support Java serialization yet so anything that depends on
`akka.serialization.JavaSerializer` will not work with `native-image`.

### Logging
It is currently not easy to get Logback working because of its Groovy dependencies and incomplete
classpath problems with `native-image` so `java.util.logging` is used instead.
