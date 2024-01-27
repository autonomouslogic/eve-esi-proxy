FROM ubuntu:22.04 as build
ARG ESI_PROXY_VERSION
RUN apt-get update && apt-get install -y zip
COPY build/distributions/esi-proxy-${ESI_PROXY_VERSION}.tar /esi-proxy.tar
RUN mkdir /esi-proxy
RUN tar -x --strip-components=1 -f /esi-proxy.tar -C /esi-proxy
COPY docker/entry.sh /esi-proxy/

FROM eclipse-temurin:17.0.8.1_1-jre-alpine
ARG ESI_PROXY_VERSION
ENV ESI_PROXY_VERSION=${ESI_PROXY_VERSION}
COPY --from=build /esi-proxy /esi-proxy
WORKDIR /esi-proxy
ENTRYPOINT ["./entry.sh"]
