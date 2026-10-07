# 3. Docker and container images

## 3.1 Concepts in two minutes

| Term | Meaning here |
|---|---|
| **Image** | Read-only package: OS libraries + JRE + our jar. Built once, run anywhere. |
| **Container** | A running instance of an image (a process with its own filesystem and network). |
| **Registry** | Image storage: OpenShift's internal registry (dev), Quay.io or Amazon ECR (prod). |
| **Layer** | Each Dockerfile step adds a layer; unchanged layers are cached and not re-uploaded. |
| **Tag** | Image version label. Dev uses `latest`; prod uses the immutable **git commit SHA**. |

## 3.2 The Dockerfile, step by step

One [Dockerfile](../Dockerfile) builds any service: `--build-arg SERVICE=<module>`.

**Stage 1, `build`** (`ubi9/openjdk-17`, full JDK):

1. Copies only `mvnw`, `.mvn/` and the `pom.xml` files, then downloads dependencies → cached layer;
   code edits do not re-download the internet.
2. Copies the sources and runs `./mvnw -pl $SERVICE -am package -DskipTests` (tests already ran in CI).
3. Splits the Spring Boot jar into **layers**: `java -Djarmode=tools -jar app.jar extract --layers`.

**Stage 2, runtime** (`ubi9/openjdk-17-runtime`, JRE only, ~200 MB smaller):

1. Copies the layers in order: `dependencies` → `spring-boot-loader` → `snapshot-dependencies` → `application`.
   A code change only replaces the last, tiny layer (a few hundred KB instead of ~75 MB).
2. `chgrp -R 0 … && chmod -R g=u …`: **OpenShift runs containers with a random UID** in group 0,
   so files must be group-readable/writable. An image that assumes "runs as root" or "runs as UID 1000"
   fails on OpenShift.
3. `USER 185`: never root.
4. `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=… -XX:+ExitOnOutOfMemoryError`: heap sized from the container's
   memory limit; on OOM the JVM exits so Kubernetes restarts it, instead of limping on.

Why **Red Hat UBI** base images? Freely redistributable, security-patched by Red Hat, supported on OpenShift,
and the registry is reachable from every ROSA cluster.

## 3.3 Four ways to build the images

| Option | Needs locally | Command | Best for |
|---|---|---|---|
| **OpenShift BuildConfig** | only `oc` | `oc start-build identity-service --from-repo=.` | You (no Docker on the PC); dev cluster |
| **Jib** (Maven plugin) | only Java | `.\mvnw.cmd -Pjib -pl identity-service -am package -Dimage.registry=quay.io/you` | CI pipelines; no Docker daemon, reproducible |
| **Docker** | Docker Desktop | `docker build --build-arg SERVICE=identity-service -t identity-service .` | Classic workflow |
| **Podman** | Podman Desktop | `podman build --build-arg SERVICE=identity-service -t identity-service .` | Rootless, daemonless Docker alternative (Red Hat) |

### Jib details

Jib is configured in the parent [pom.xml](../pom.xml) (`jib` profile). It builds the same layered,
non-root (UID 185) UBI image **without a Dockerfile and without Docker**, and pushes directly:

```powershell
# log in once: Jib reuses Docker/Podman credential helpers, or pass credentials explicitly
.\mvnw.cmd -Pjib -pl identity-service -am package -DskipTests `
  -Dimage.registry=quay.io/your-org -Dimage.tag=1.0.0 `
  "-Djib.to.auth.username=your-org+robot" "-Djib.to.auth.password=…"
```

## 3.4 Run a single image

```bash
docker run --rm -p 8081:8080 -e JWT_SECRET=local-dev-only-secret-change-me-0123456789 identity-service
# no DB_URL → H2 in memory with demo data
curl localhost:8081/actuator/health
```

## 3.5 Run the whole stack with docker-compose

[docker-compose.yml](../docker-compose.yml) starts MySQL 8.4 (with one database + user per service from
[deploy/db/init-databases.sql](../deploy/db/init-databases.sql)) and all five services:

```powershell
docker compose up --build            # or: podman compose up --build
.\scripts\verify-deployment.ps1 -BaseUrl http://localhost:8080
docker compose down -v               # stop and delete the MySQL volume
```

Needs about 3 GB free RAM.

## 3.6 Image hygiene checklist (production)

- [x] Multi-stage build: no JDK, Maven or sources in the runtime image
- [x] Non-root user, works with arbitrary UIDs (OpenShift `restricted-v2` SCC)
- [x] No secrets in the image: everything comes from environment variables at runtime
- [x] `.dockerignore` keeps `.git`, `target/` and the legacy SQL dump out of the build context
- [x] Immutable tags in prod (`:<git-sha>`), never deploy `:latest` to prod
- [ ] Scan images: Quay does it automatically; or `podman run --rm aquasec/trivy image <image>`
- [ ] Sign images (Sigstore / `cosign`) and enforce signatures with Red Hat Advanced Cluster Security
