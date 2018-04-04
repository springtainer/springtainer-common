springboot-testcontainers-common
================================

[![Maven Central](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/avides/springboot/testcontainer/springboot-testcontainer-common/maven-metadata.xml.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.avides.springboot.testcontainer%22%20AND%20a%3A%22springboot-testcontainer-common%22)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f23e7fe358f44755b4b006178eb3dc8c)](https://www.codacy.com/app/avides-builds/springboot-testcontainer-common?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=springboot-testcontainer/springboot-testcontainer-common&amp;utm_campaign=Badge_Grade)
[![Coverage Status](https://coveralls.io/repos/springboot-testcontainer/springboot-testcontainer-common/badge.svg)](https://coveralls.io/r/springboot-testcontainer/springboot-testcontainer-common)
[![Build Status](https://travis-ci.org/springboot-testcontainer/springboot-testcontainer-common.svg?branch=master)](https://travis-ci.org/springboot-testcontainer/springboot-testcontainer-common)

### Dependency
```xml
<dependency>
	<groupId>com.avides.springboot.testcontainer</groupId>
	<artifactId>springboot-testcontainer-common</artifactId>
	<version>0.1.0-RC2</version>
</dependency>
```

### Configuration
Properties consumed (in `bootstrap.properties`):
- `embedded.container-network` (default is `bridge`)
- `embedded.stale-containers-cleanup.enabled` (default is `true`)
- `embedded.stale-containers-cleanup.after-minutes` (default is `10`)
