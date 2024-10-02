FROM circleci/openjdk:11.0.2-jdk

USER root

RUN set -x
RUN  rm -rf /var/lib/apt/lists/* && apt-get update && apt-get install -y openjdk-8-jdk

USER circleci

ENV JAVA_1_8_HOME /usr/lib/jvm/java-8-openjdk-amd64
