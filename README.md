# springtainer-common

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.avides.springboot.springtainer/springtainer-common/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.avides.springboot.springtainer/springtainer-common)
[![Build](https://github.com/springtainer/springtainer-common/workflows/release/badge.svg)](https://github.com/springtainer/springtainer-common/actions)
[![Nightly build](https://github.com/springtainer/springtainer-common/workflows/nightly/badge.svg)](https://github.com/springtainer/springtainer-common/actions)
[![Coverage report](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=coverage)](https://sonarcloud.io/dashboard?id=springtainer_springtainer-common)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=alert_status)](https://sonarcloud.io/dashboard?id=springtainer_springtainer-common)
[![Technical dept](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=sqale_index)](https://sonarcloud.io/dashboard?id=springtainer_springtainer-common)

### Dependency
```xml
<dependency>
	<groupId>com.avides.springboot.springtainer</groupId>
	<artifactId>springtainer-common</artifactId>
	<version>1.3.0-SNAPSHOT</version>
</dependency>
```

### Configuration
Properties consumed (in `bootstrap.properties`):
- `embedded.container.common.network` (default is `bridge`)
- `embedded.container.cleanup.enabled` (default is `true`)
- `embedded.container.cleanup.after-minutes` (default is `10`)
- `embedded.container.cleanup.max-concurrent-per-issuer` (default is `10`)
- `embedded.container.mac.localhost.host` (default is `127.0.0.1`)
