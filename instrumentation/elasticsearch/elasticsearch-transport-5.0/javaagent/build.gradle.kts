plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.elasticsearch.client")
    module.set("transport")
    versions.set("[5.0.0,5.3.0)")
    // version 7.11.0 depends on org.elasticsearch:elasticsearch:7.11.0 which depends on
    // org.elasticsearch:elasticsearch-plugin-classloader:7.11.0 which does not exist
    skip("7.11.0")
    assertInverse.set(true)
  }
  pass {
    group.set("org.elasticsearch")
    module.set("elasticsearch")
    versions.set("[5.0.0,5.3.0)")
    // version 7.11.0 depends on org.elasticsearch:elasticsearch:7.11.0 which depends on
    // org.elasticsearch:elasticsearch-plugin-classloader:7.11.0 which does not exist
    skip("7.11.0")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.elasticsearch.client:transport:5.0.0")

  implementation(project(":instrumentation:elasticsearch:elasticsearch-transport-common:library"))

  // Ensure no cross interference
  testInstrumentation(project(":instrumentation:elasticsearch:elasticsearch-rest-5.0:javaagent"))
  testInstrumentation(project(":instrumentation:apache-httpasyncclient-4.1:javaagent"))
  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))

  testImplementation(project(":instrumentation:elasticsearch:elasticsearch-transport-common:testing"))
  testImplementation("org.apache.logging.log4j:log4j-core:2.11.0")
  testImplementation("org.apache.logging.log4j:log4j-api:2.11.0")

  testLibrary("org.elasticsearch.plugin:transport-netty3-client:5.0.0")
  testLibrary("org.elasticsearch.client:transport:5.0.0")

  latestDepTestLibrary("org.elasticsearch.plugin:transport-netty3-client:5.0.0")
  latestDepTestLibrary("org.elasticsearch.client:transport:5.0.0")
}

tasks.withType<Test>().configureEach {
  // TODO run tests both with and without experimental span attributes
  jvmArgs("-Dotel.instrumentation.elasticsearch.experimental-span-attributes=true")
  // required on jdk17
  jvmArgs("--add-opens=java.base/java.nio=ALL-UNNAMED")
  jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")
}
