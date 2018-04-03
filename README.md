springboot-testcontainers-common
================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.avides.springboot.testcontainer/springboot-testcontainer-common/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.avides.springboot.testcontainer/springboot-testcontainer-common)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f23e7fe358f44755b4b006178eb3dc8c)](https://www.codacy.com/app/avides-builds/springboot-testcontainer-common)
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
