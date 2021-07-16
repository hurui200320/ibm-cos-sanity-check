FROM gradle:7.1.1-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist

FROM adoptopenjdk/openjdk11 AS runtime
RUN mkdir /opt/app
COPY --from=build /home/gradle/src/build/install/ibm-cos-sanity-check /opt/app
WORKDIR /opt/app
CMD ["bash", "./bin/ibm-cos-sanity-check"]

