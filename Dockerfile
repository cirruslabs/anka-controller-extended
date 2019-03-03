FROM gradle:5.2-jdk11 as builder

USER root
WORKDIR /tmp/build
ADD . /tmp/build/

RUN ./gradlew clean :impl:shadowJar --stacktrace --no-daemon

FROM openjdk:11

MAINTAINER Fedor fedor.korotkov@gmail.com;

WORKDIR /svc/anka-controller-extended
ADD scripts/entrypoint.sh /svc/anka-controller-extended

RUN apt-get update && apt-get install -y --no-install-recommends \
		openconnect \
	&& rm -rf /var/lib/apt/lists/*

HEALTHCHECK CMD curl --fail http://localhost:8081/healthcheck || exit 1

EXPOSE 8080
EXPOSE 8081
EXPOSE 8239

COPY --from=builder /tmp/build/impl/config/anka_controller_extended_config.yml /svc/anka-controller-extended
COPY --from=builder /tmp/build/impl/build/libs/anka-controller-extended-all.jar /svc/anka-controller-extended

CMD ./entrypoint.sh
