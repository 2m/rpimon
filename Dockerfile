FROM bellsoft/liberica-openjdk-alpine:21

RUN apk add --no-cache busctl

ADD target/rpimon.jar /data/rpimon.jar

CMD ["java", "-jar", "/data/rpimon.jar"]
