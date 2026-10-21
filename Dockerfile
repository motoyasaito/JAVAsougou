FROM eclipse-temurin:17-jdk

RUN mkdir -p /app
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends maven inotify-tools \
  && rm -rf /var/lib/apt/lists/*

COPY pom.xml /app
RUN mvn dependency:go-offline

COPY src /app/src

RUN mvn clean install -DskipTests

ENV JAVA_OPTS="-Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true"

CMD ["sh", "-c", "(while inotifywait -q -r -e modify,create,delete,move /app/src; do echo '--- ソース変更を検知: 再コンパイルします ---'; mvn -q compile; done) & mvn spring-boot:run -Dspring-boot.run.profiles=dev $JAVA_OPTS"]