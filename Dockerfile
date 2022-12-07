FROM gradle:6.9.0-jdk11 AS build
ENV DOCKER_ENV=dev
ARG jfrog_password_arg
ENV jfrog_password=$jfrog_password_arg
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build --no-daemon

FROM eclipse-temurin:11-jre
RUN apt-get update && apt-get install -y curl dnsutils iputils-ping
RUN mkdir -p /apps/OpsERA/components/argo-integrator
COPY --from=build /home/gradle/src/build/libs/*.jar /apps/OpsERA/components/argo-integrator/argo-integrator.jar
ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
EXPOSE 9096
ENTRYPOINT exec /tini -- java -XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -Dspring.profiles.active=$DOCKER_ENV $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /apps/OpsERA/components/argo-integrator/argo-integrator.jar