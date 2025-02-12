# pod4dev-java
Podman Containers for Java

Environment variables:

```dotenv
PODMAN_HOST=unix:///var/run/user/1000/podman/podman.sock
```

Order to read the environment variables: 
1. `PODMAN_HOST`
2. `DOCKER_HOST`

An example you can see in tests:
- [`test.yaml`][1]
- [`KubePlayerTest.java`][2]

[1]: src/test/resources/test.yaml
[2]: src/test/java/io/github/pod4dev/java/KubePlayerTest.java