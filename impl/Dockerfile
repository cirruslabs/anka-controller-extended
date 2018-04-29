FROM openjdk:8

MAINTAINER Fedor fedor.korotkov@gmail.com;

RUN apt-get update && apt-get install -y --no-install-recommends \
		openconnect \
	&& rm -rf /var/lib/apt/lists/*

HEALTHCHECK  --interval=10s --timeout=10s --start-period=10s \
  CMD /sbin/ifconfig tun0

WORKDIR /svc/anka-controller-extended

EXPOSE 8080
EXPOSE 8239

ADD anka_controller_extended_config.yml /svc/anka-controller-extended
ADD anka-controller-extended-all.jar /svc/anka-controller-extended

CMD ip tuntap add vpn0 mode tun && \
    ( echo yes; echo $ANYCONNECT_PASSWORD ) | openconnect $ANYCONNECT_SERVER --user=$ANYCONNECT_USER --timestamp --background && \
    exec java -jar anka-controller-extended-all.jar server /svc/anka-controller-extended/anka_controller_extended_config.yml
