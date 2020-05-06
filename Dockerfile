FROM openjdk:11-jdk-slim

RUN mkdir /app && apt-get update && apt-get -y install wget && apt-get -y install firefox-esr && apt-get install unzip && \
    wget https://github.com/mozilla/geckodriver/releases/download/v0.26.0/geckodriver-v0.26.0-linux64.tar.gz && \
    tar -zxf geckodriver-v0.26.0-linux64.tar.gz -C /usr/bin

WORKDIR /app

COPY ./build/libs/peapod-scraper-all.jar /app/

CMD ["java", "-jar", "peapod-scraper-all.jar"]
