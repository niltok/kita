FROM bellsoft/liberica-openjdk-debian:19 AS TEMP_BUILD_IMAGE
ARG MODE=prod
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY build.gradle gradle.properties settings.gradle gradlew $APP_HOME/
COPY gradle $APP_HOME/gradle
COPY api/build.gradle $APP_HOME/api/
COPY manager/build.gradle $APP_HOME/manager/
COPY client/package.json client/package-lock.json $APP_HOME/client/

RUN ./gradlew manager:cache --info --stacktrace -Dmode=${MODE}
COPY . .
RUN ./gradlew manager:buildAll --info --stacktrace -Dmode=${MODE}

FROM bellsoft/liberica-openjdk-debian:19
ENV ARTIFACT_NAME=manager-rolling-all.jar
ENV APP_HOME=/usr/app/

WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/manager/build/libs/$ARTIFACT_NAME ./
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/client/dist webroot

EXPOSE 8070
ENTRYPOINT exec java --enable-preview -jar ${ARTIFACT_NAME} -cluster