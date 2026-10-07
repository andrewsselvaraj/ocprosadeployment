# One Dockerfile for every service. Pick the module with the SERVICE build argument:
#
#   docker build --build-arg SERVICE=identity-service -t identity-service:dev .
#   podman build --build-arg SERVICE=exam-service     -t exam-service:dev .
#   oc start-build identity-service --from-repo=. --follow     (OpenShift BuildConfig, see deploy/openshift)
#
# Stage 1 builds the jar with the Maven Wrapper; stage 2 is a small JRE-only runtime image.

# ---------- build stage ----------
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build
ARG SERVICE
USER root
WORKDIR /workspace

# Copy the build definition first so the dependency download is cached as its own layer
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY common-lib/pom.xml common-lib/
COPY identity-service/pom.xml identity-service/
COPY academic-service/pom.xml academic-service/
COPY question-service/pom.xml question-service/
COPY exam-service/pom.xml exam-service/
COPY api-gateway/pom.xml api-gateway/
RUN chmod +x mvnw && ./mvnw -B -q -pl ${SERVICE} -am dependency:go-offline -DexcludeArtifactIds=common-lib || true

COPY . .
# Tests run in the CI pipeline before the image is built, so they are skipped here
RUN ./mvnw -B -q -pl ${SERVICE} -am package -DskipTests \
 && cp ${SERVICE}/target/${SERVICE}-*.jar /workspace/application.jar \
 && java -Djarmode=tools -jar /workspace/application.jar extract --layers --destination /workspace/extracted

# ---------- runtime stage ----------
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest
ARG SERVICE
LABEL org.opencontainers.image.title="${SERVICE}" \
      org.opencontainers.image.source="https://github.com/your-org/ocprosadeployment" \
      io.openshift.tags="java,spring-boot,m2its"

WORKDIR /deployments
# Layers ordered from least to most frequently changing: a code change only rebuilds the last one
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# OpenShift runs the container with a random UID that belongs to group 0 (root group),
# so files must be group-readable. Never rely on running as a specific user.
USER root
RUN chgrp -R 0 /deployments && chmod -R g=u /deployments
USER 185

EXPOSE 8080
# Container-aware JVM: heap sized from the container memory limit, crash fast on OOM so
# Kubernetes restarts the pod.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "/deployments/application.jar"]
