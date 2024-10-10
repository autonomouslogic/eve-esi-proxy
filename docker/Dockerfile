FROM ubuntu:24.04 as build
ARG ESI_PROXY_VERSION
COPY build/distributions/eve-esi-proxy-${ESI_PROXY_VERSION}.tar /esi-proxy.tar
RUN mkdir /esi-proxy
RUN tar -x --strip-components=1 -f /esi-proxy.tar -C /esi-proxy
COPY docker/entry.sh /esi-proxy/

FROM eclipse-temurin:21.0.4_7-jre-alpine
ARG ESI_PROXY_VERSION
ENV ESI_PROXY_VERSION=${ESI_PROXY_VERSION}
ENV ESI_PROXY_HTTP_CACHE_DIR=/data/http-cache
COPY --from=build /esi-proxy /esi-proxy
WORKDIR /esi-proxy
ENTRYPOINT ["./entry.sh"]
