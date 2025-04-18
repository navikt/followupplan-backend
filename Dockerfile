FROM gcr.io/distroless/java21-debian12@sha256:903d5ad227a4afff8a207cd25c580ed059cc4006bb390eae65fb0361fc9724c3
WORKDIR /app
COPY build/libs/app.jar app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app.jar" ]
