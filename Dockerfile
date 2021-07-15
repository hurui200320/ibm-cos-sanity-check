FROM adoptopenjdk/openjdk11
RUN mkdir /opt/app
COPY build/install/ibm-cos-sanity-check /opt/app
WORKDIR /opt/app
CMD ["bash", "./bin/ibm-cos-sanity-check"]

