FROM openjdk:15-jdk-alpine3.12
RUN addgroup -S metis && adduser -S metis -G metis
USER metis:metis
RUN mkdir /home/metis/reports
RUN chown metis:metis /home/metis/reports
RUN mkdir /home/metis/images
RUN chown metis:metis /home/metis/images
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-DSPRING_PROFILES_ACTIVE=docr","-jar","/app.jar"]
