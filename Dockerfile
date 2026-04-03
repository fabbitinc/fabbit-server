FROM ubuntu:22.04 AS converter-builder

ARG DEBIAN_FRONTEND=noninteractive
ARG MAYO_TAG=v0.9.0

RUN apt-get update && apt-get install -y \
    cmake git g++ \
    qtbase5-dev libqt5svg5-dev libxcb-cursor0 \
    libocct-data-exchange-dev libocct-draw-dev occt-misc libtbb2-dev libxi-dev \
    libassimp-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src
RUN git clone --branch ${MAYO_TAG} --depth 1 https://github.com/fougue/mayo.git

WORKDIR /build
RUN cmake /src/mayo -DCMAKE_BUILD_TYPE=Release -DMayo_BuildPluginAssimp=ON
RUN cmake --build . --config Release -j"$(nproc)"

# 
# 
# 

FROM eclipse-temurin:21-jdk-jammy AS java-builder

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties gradle.lockfile ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon bootJar

# 
# 
# 

FROM eclipse-temurin:21-jre-jammy

ARG DEBIAN_FRONTEND=noninteractive
ARG EZDXF_VERSION=1.4.3

RUN apt-get update && apt-get install -y \
    python3 python3-pip \
    qtbase5-dev libqt5svg5-dev libxcb-cursor0 \
    libocct-data-exchange-dev libocct-draw-dev occt-misc libtbb2-dev libxi-dev \
    libassimp-dev xvfb \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m pip install --no-cache-dir "ezdxf[draw]==${EZDXF_VERSION}"

COPY --from=converter-builder /build/mayo-conv /opt/mayo-conv

WORKDIR /app

ENV PORT=10010
ENV SPRING_PROFILES_ACTIVE=prod
ENV EZDXF_BIN_PATH=/usr/local/bin/ezdxf
ENV THREE_D_CONVERTER_BIN_PATH=/opt/mayo-conv

COPY --from=java-builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 10010

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
