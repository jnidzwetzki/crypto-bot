# Build with: docker build --build-arg version=0.0.1 -t jnidzwetzki/crypto-bot:0.0.1 - < Dockerfile

FROM alpine/git as clone
ARG version
WORKDIR /crypto-bot
RUN git clone https://github.com/jnidzwetzki/crypto-bot.git /crypto-bot
RUN git checkout tags/v${version}

FROM maven:3.5-jdk-8-alpine as build
WORKDIR /crypto-bot
COPY --from=clone /crypto-bot /crypto-bot
RUN mvn install

FROM openjdk:8-jre-alpine
WORKDIR /crypto-bot
COPY --from=build /crypto-bot /crypto-bot 
ENTRYPOINT ["sh", "-c"]
CMD ["/crypto-bot/utils/run_donchian_bot.sh"]
