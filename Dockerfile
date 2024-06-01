FROM bellsoft/liberica-openjdk-alpine:21

ADD target/rpimon.jar /data/rpimon.jar

CMD ["java", "-jar", "/data/rpimon.jar"]
