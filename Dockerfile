FROM ubuntu:bionic

# Create a working build directory
WORKDIR /var/local/pipeline

# Use a bash shell instead of /bin/sh
SHELL ["/bin/bash", "-c"] 

# Update and upgrade apt-get and install utils
RUN apt-get -qq update && apt-get -qq upgrade -y > /dev/null; \
	apt-get -qq install -y \
	apt-utils

# Install package dependencies
RUN apt-get -qq install -y \
    apt-transport-https \
    build-essential \
    ca-certificates \
    curl \
    git \
    gnupg \
    openjdk-11-jdk \
    unzip \
    wget \
    zip

# Install gcloud utilities
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" \
    | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
    && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | \
    apt-key --keyring /usr/share/keyrings/cloud.google.gpg  add - \
    && apt-get update -y \
    && apt-get install google-cloud-cli -y

# Install SDKMAN for ease of installing Gradle
RUN curl -s "https://get.sdkman.io" | bash
RUN source /root/.sdkman/bin/sdkman-init.sh; \
    sdk version; \
    sdk install gradle 6.3 < /dev/null
ENV PATH="${PATH}:/root/.sdkman/candidates/gradle/current/bin"

# Install Golang
RUN wget -O /tmp/go1.14.15.linux-amd64.tar.gz https://go.dev/dl/go1.14.15.linux-amd64.tar.gz \
    && rm -rf /usr/local/go \
    && tar -C /usr/local -xzf /tmp/go1.14.15.linux-amd64.tar.gz
ENV PATH="${PATH}:/usr/local/go/bin"

# Install protoc
RUN wget -O /tmp/protoc-3.14.0-linux-x86_64.zip https://github.com/protocolbuffers/protobuf/releases/download/v3.14.0/protoc-3.14.0-linux-x86_64.zip \
    && unzip /tmp/protoc-3.14.0-linux-x86_64.zip -d /tmp/protoc
RUN mv /tmp/protoc/bin/protoc /usr/local/bin \
    && mv /tmp/protoc/include/* /usr/local/include

# Copy source files into the container
COPY . .

# Build the project
RUN gradle wrapper --gradle-version 6.3
RUN ./gradlew shadowJar