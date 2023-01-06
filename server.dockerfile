FROM bellsoft/liberica-openjdk-debian:19 AS TEMP_BUILD_IMAGE
ARG MODE=prod
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY build.gradle gradle.properties settings.gradle gradlew $APP_HOME/
RUN chmod a+x gradlew
COPY gradle $APP_HOME/gradle
COPY api/build.gradle $APP_HOME/api/
COPY server/build.gradle $APP_HOME/server/

RUN ./gradlew server:cache --info --stacktrace -Dmode=${MODE}
COPY . .
RUN ./gradlew server:shadowJar --info --stacktrace -Dmode=${MODE}

FROM bellsoft/liberica-openjdk-debian:19
ENV ARTIFACT_NAME=server-rolling-all.jar
ENV APP_HOME=/usr/app/

WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/server/build/libs/$ARTIFACT_NAME ./

ENTRYPOINT exec java --enable-preview -jar ${ARTIFACT_NAME} -cluster