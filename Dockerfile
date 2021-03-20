FROM gradle:6.3.0-jdk8 AS build
ENV DOCKER_ENV=dev
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build --no-daemon

FROM openjdk:8-jre-slim
RUN apt-get update && apt-get install -y curl dnsutils iputils-ping
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /apps/OpsERA/components/argo-integrator/argo-integrator.jar
EXPOSE 9096
ENTRYPOINT java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dspring.profiles.active=$DOCKER_ENV -Djava.security.egd=file:/dev/./urandom -jar /apps/OpsERA/components/argo-integrator/argo-integrator.jar