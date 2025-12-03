FROM ubuntu:24.04 as build
ARG PROXY_VERSION
COPY build/distributions/eve-esi-proxy-${PROXY_VERSION}.tar /eve-esi-proxy.tar
RUN mkdir /eve-esi-proxy
RUN tar -x --strip-components=1 -f /eve-esi-proxy.tar -C /eve-esi-proxy
COPY docker/entry.sh /eve-esi-proxy/

FROM eclipse-temurin:21.0.9_10-jre-alpine
ENV CONFIG_DIR=/data
ENV HTTP_CACHE_DIR=/data/http-cache
COPY --from=build /eve-esi-proxy /eve-esi-proxy
WORKDIR /eve-esi-proxy
ENTRYPOINT ["./entry.sh"]
