FROM openjdk:11-jre-slim
EXPOSE 8901
ADD target/apoiador-requisitante*.jar bot-apoiador-requisitante.jar
ENTRYPOINT ["java","-jar","/bot-apoiador-requisitante.jar", "--logging.file=/tmp/bot-apoiador-requisitante.log"]