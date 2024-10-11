FROM ubuntu:24.04 as build
ARG EVE_ESI_PROXY_VERSION
RUN echo Version: ${EVE_ESI_PROXY_VERSION}
COPY build/distributions/eve-esi-proxy-${EVE_ESI_PROXY_VERSION}.tar /eve-esi-proxy.tar
RUN mkdir /eve-esi-proxy
RUN tar -x --strip-components=1 -f /eve-esi-proxy.tar -C /eve-esi-proxy
COPY docker/entry.sh /eve-esi-proxy/

FROM eclipse-temurin:21.0.4_7-jre-alpine
ARG EVE_ESI_PROXY_VERSION
ENV EVE_ESI_PROXY_VERSION=${EVE_ESI_PROXY_VERSION}
ENV EVE_ESI_PROXY_HTTP_CACHE_DIR=/data/http-cache
COPY --from=build /eve-esi-proxy /eve-esi-proxy
WORKDIR /eve-esi-proxy
ENTRYPOINT ["./entry.sh"]
