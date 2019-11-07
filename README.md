# springboot-testcontainers-common

[![Maven Central](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/avides/springboot/testcontainer/springboot-testcontainer-common/maven-metadata.xml.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.avides.springboot.testcontainer%22%20AND%20a%3A%22springboot-testcontainer-common%22)
[![Build](https://github.com/springboot-testcontainer/springboot-testcontainer-common/workflows/release/badge.svg)](https://github.com/springboot-testcontainers/springboot-testcontainers-common/actions)
[![Nightly build](https://github.com/springboot-testcontainer/springboot-testcontainer-common/workflows/nightly/badge.svg)](https://github.com/springboot-testcontainers/springboot-testcontainers-common/actions)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f23e7fe358f44755b4b006178eb3dc8c)](https://www.codacy.com/app/avides-builds/springboot-testcontainer-common)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/f23e7fe358f44755b4b006178eb3dc8c)](https://www.codacy.com/app/avides-builds/springboot-testcontainer-common)

### Dependency
```xml
<dependency>
	<groupId>com.avides.springboot.testcontainer</groupId>
	<artifactId>springboot-testcontainer-common</artifactId>
	<version>1.0.0</version>
</dependency>
```

### Configuration
Properties consumed (in `bootstrap.properties`):
- `embedded.container.common.network` (default is `bridge`)
- `embedded.container.cleanup.enabled` (default is `true`)
- `embedded.container.cleanup.after-minutes` (default is `10`)
- `embedded.container.cleanup.max-concurrent-per-issuer` (default is `10`)
