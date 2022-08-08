FROM gradle:6.9.0-jdk11 AS build
ENV DOCKER_ENV=dev
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build --no-daemon

FROM openjdk:11-jre-slim
RUN apt-get update && apt-get install -y curl dnsutils iputils-ping
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /apps/OpsERA/components/argo-integrator/argo-integrator.jar
ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
EXPOSE 9096
ENTRYPOINT exec /tini -- java -XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -Dspring.profiles.active=$DOCKER_ENV -Djava.security.egd=file:/dev/./urandom -jar /apps/OpsERA/components/argo-integrator/argo-integrator.jar