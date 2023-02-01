chmod 777 ./*.jar
java --enable-preview --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -Xlog:gc -XX:+UseZGC -XX:+PrintCommandLineFlags $(echo $1 | tr ";;" "\n") -jar $(echo $2 | tr ";;" "\n")
