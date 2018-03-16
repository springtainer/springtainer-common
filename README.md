springboot-testcontainers-common
================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.avides.springboot.testcontainer/springboot-testcontainer-common/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.avides.springboot.testcontainer/springboot-testcontainer-common)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/xxx)](https://www.codacy.com/app/springboot-testcontainer/springboot-testcontainer-common)
[![Coverage Status](https://coveralls.io/repos/springboot-testcontainer/springboot-testcontainer-common/badge.svg)](https://coveralls.io/r/springboot-testcontainer/springboot-testcontainer-common)
[![Build Status](https://travis-ci.org/springboot-testcontainer/springboot-testcontainer-common.svg?branch=master)](https://travis-ci.org/springboot-testcontainer/springboot-testcontainer-common)

### Dependency
```xml
<dependency>
	<groupId>com.avides.springboot.testcontainer</groupId>
	<artifactId>springboot-testcontainer-common</artifactId>
	<version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Configuration
Properties consumed (in `bootstrap.properties`):
- `embedded.container-network` (default is `bridge`)
- `embedded.stale-containers-cleanup.enabled` (default is `true`)
- `embedded.stale-containers-cleanup.after-minutes` (default is `10`)
