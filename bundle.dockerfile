FROM bellsoft/liberica-openjdk-debian:19 AS TEMP_BUILD_IMAGE
ARG MODE=prod
ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME
COPY build.gradle gradle.properties settings.gradle gradlew $APP_HOME/
RUN chmod a+x gradlew
COPY gradle $APP_HOME/gradle
COPY api/build.gradle $APP_HOME/api/
COPY manager/build.gradle $APP_HOME/manager/
COPY client/package.json client/package-lock.json $APP_HOME/client/
COPY server/build.gradle $APP_HOME/server/
COPY bundle/build.gradle $APP_HOME/bundle/build.gradle

RUN ./gradlew bundle:cache --info --stacktrace -Dmode=${MODE}
COPY . .
RUN chmod a+x gradlew
RUN ./gradlew bundle:shadowJar --info --stacktrace -Dmode=${MODE}

FROM bellsoft/liberica-openjre-alpine:19
ENV ARTIFACT_NAME=bundle-rolling-all.jar
ENV APP_HOME=/usr/app/

WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/manager/build/libs/$ARTIFACT_NAME ./

EXPOSE 8070
ENTRYPOINT exec java --enable-preview -jar ${ARTIFACT_NAME} -cluster