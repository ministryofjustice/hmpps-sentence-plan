FROM wiremock/wiremock:3.7.0

# Download the wiremock-faker-extension
ENV FAKER_EXTENSION_VERSION=0.2.0
ADD https://repo1.maven.org/maven2/org/wiremock/extensions/wiremock-faker-extension-standalone/${FAKER_EXTENSION_VERSION}/wiremock-faker-extension-standalone-${FAKER_EXTENSION_VERSION}.jar /var/wiremock/extensions

# Expose the default WireMock port
EXPOSE 8080

CMD ["--global-response-templating", "--extensions", "org.wiremock.RandomExtension"]