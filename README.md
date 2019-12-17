# springtainer-common

[![Maven Central](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/avides/springboot/testcontainer/springtainer-common/maven-metadata.xml.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.avides.springboot.testcontainer%22%20AND%20a%3A%22springtainer-common%22)
[![Build](https://github.com/springtainer/springtainer-common/workflows/release/badge.svg)](https://github.com/springtainer/springtainer-common/actions)
[![Nightly build](https://github.com/springtainer/springtainer-common/workflows/nightly/badge.svg)](https://github.com/springtainer/springtainer-common/actions)
[![Coverage report](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=coverage)](https://sonarcloud.io/dashboard?id=springtainer_springtainer-common)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=alert_status)](https://sonarcloud.io/dashboard?id=springtainer_springtainer-common)
[![Technical dept](https://sonarcloud.io/api/project_badges/measure?project=springtainer_springtainer-common&metric=sqale_index)](https://sonarcloud.io/dashboard?id=springtainer_springtainer-common)

### Dependency
```xml
<dependency>
	<groupId>com.avides.springboot.testcontainer</groupId>
	<artifactId>springtainer-common</artifactId>
	<version>1.0.0</version>
</dependency>
```

### Configuration
Properties consumed (in `bootstrap.properties`):
- `embedded.container.common.network` (default is `bridge`)
- `embedded.container.cleanup.enabled` (default is `true`)
- `embedded.container.cleanup.after-minutes` (default is `10`)
- `embedded.container.cleanup.max-concurrent-per-issuer` (default is `10`)
