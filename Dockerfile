FROM eclipse-temurin:21-jdk
WORKDIR /workspace/thaumcraft
ENV GRADLE_USER_HOME=/workspace/thaumcraft/.gradle21
ENTRYPOINT ["./gradlew"]
CMD ["--no-daemon", "--console=plain", ":common:test", ":fabric:build", ":neoforge:build"]
