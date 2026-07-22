# springtainer-common

[![Maven Central](https://img.shields.io/maven-central/v/com.avides.springboot.springtainer/springtainer-common.svg?label=maven-central)](https://search.maven.org/artifact/com.avides.springboot.springtainer/springtainer-common)
[![Release](https://github.com/springtainer/springtainer-common/actions/workflows/release.yml/badge.svg)](https://github.com/springtainer/springtainer-common/actions/workflows/release.yml)
[![Nightly build](https://github.com/springtainer/springtainer-common/actions/workflows/nightly.yml/badge.svg)](https://github.com/springtainer/springtainer-common/actions/workflows/nightly.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=springtainer_springtainer-common)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=coverage)](https://sonarcloud.io/summary/new_code?id=springtainer_springtainer-common)

### Dependency
```xml
<dependency>
  <groupId>com.avides.springboot.springtainer</groupId>
  <artifactId>springtainer-common</artifactId>
  <version>2.0.0-RC6</version>
</dependency>
```

### Configuration
Properties consumed (in `bootstrap.properties`):
- `embedded.container.common.network` (default is `bridge`)
- `embedded.container.cleanup.enabled` (default is `true`)
- `embedded.container.cleanup.after-minutes` (default is `10`)
- `embedded.container.cleanup.max-concurrent-per-issuer` (default is `10`)
- `embedded.container.mac.localhost.host` (default is `127.0.0.1`)

### Spring's test-context cache is bounded automatically

`spring.test.context.cache.maxSize=1` ships as a classpath `spring.properties` resource inside this module's jar
(`src/main/resources/spring.properties`), so it's picked up automatically by every consumer of any springtainer module -
no configuration needed on your side. This bounds Spring's test-context cache so a no-longer-current context (and, via
its `ContextClosedEvent` listener, the embedded container it owns) gets evicted and cleanly closed as soon as a
differently-configured context needs the slot, instead of piling up unclosed until the whole JVM exits.

This works the same way whether tests are launched via Maven Surefire/Failsafe or directly from an IDE's own test
runner (e.g. Eclipse), since Spring resolves it from the classpath (`org.springframework.core.SpringProperties`) rather
than from a JVM system property.
