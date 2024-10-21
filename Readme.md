# EVE ESI Proxy
An HTTP proxy specifically designed for the [ESI API](https://esi.evetech.net/ui/) for [EVE Online](https://www.eveonline.com/).

![GitHub branch status](https://img.shields.io/github/checks-status/autonomouslogic/eve-esi-proxy/main)
![Latest Release](https://img.shields.io/github/v/release/autonomouslogic/eve-esi-proxy)
![GitHub last commit](https://img.shields.io/github/last-commit/autonomouslogic/eve-esi-proxy)
![License](https://img.shields.io/github/license/autonomouslogic/eve-esi-proxy)
![Docker Pulls](https://img.shields.io/docker/pulls/autonomouslogic/eve-esi-proxy)
![Docker Image Size (latest by date)](https://img.shields.io/docker/image-size/autonomouslogic/eve-esi-proxy)
[![Code Coverage](https://codecov.io/gh/autonomouslogic/eve-esi-proxy/graph/badge.svg?token=MXwjEUJRPk)](https://codecov.io/gh/autonomouslogic/eve-esi-proxy)
[![CodeClimate Maintainability](https://api.codeclimate.com/v1/badges/a71c017cbcce32d7a595/maintainability)](https://codeclimate.com/github/autonomouslogic/eve-esi-proxy/maintainability)

The ESI API is a great resource, but can be difficult to work with.
The features below are all things you have to be acutely aware of.
With CCP being trigger-happy about banning IPs from accessing the API,
using this proxy will let you get on with writing your application and not worry about the minutiae of ESI lore.

Join us on [Discord](https://everef.net/discord).

Fly safe o7

## Features
* **Character login** is supported and OAuth is handled automatically, [see below](#character-login)
* **Cache responses** to disk to improve request times and reduce load on the ESI itself. Authed requests are not currently cached
* **Conditional requests** to refresh objects in the cache
* **Rate limiting** to help avoid being banned, including different limits for the endpoints which have special undocumented limits
* **Handle ESI error limit headers** to stop all requests if the limit is reached
* **Retry failed requests** if a 5xx is returned
* **User agent header** is automatically handled
* **Fetching multiple pages** if no page (or page 0) is set in the request, merging all pages into a single response

Caching, rate limiting, retries, etc. are all handled transparently.

## Usage
Run via Docker ([installation](https://docs.docker.com/engine/install/)):
```bash
docker run -it -v eve-esi-proxy:/data -p 8182:8182 -m 2g -e "ESI_USER_AGENT=<your email>" autonomouslogic/eve-esi-proxy:latest
```

Then you request data as you would on the ESI, just from localhost instead:
```bash
curl http://localhost:8182/latest/status/
```

## Config
The proxy is configured via environment variables set via `docker run -e`:

* `PROXY_PORT` - The port the proxy listens on - defaults to `8182`
* `PROXY_HOST` - The host the proxy listens on - defaults to `0.0.0.0`
* `ESI_BASE_URL` - The base URL of the ESI API - defaults to `https://esi.evetech.net`
* `ESI_USER_AGENT` - The user agent to use when making requests to the ESI API - if this is not set and no user agent is supplied on the request, the proxy will return a `400 Bad Request`
* `HTTP_CACHE_DIR` - The directory to store cached responses in
* `HTTP_CACHE_MAX_SIZE` - The maximum size of the cache in bytes - defaults to 1 GiB
* `HTTP_MAX_TRIES` - Maximum number of times to try a single request - defaults to `3`
* `HTTP_RETRY_DELAY` - Delay between retries - defaults to `PT2S`
* `ESI_RATE_LIMIT_PER_S` - The number of requests allowed per second for endpoints without a special rate limit - defaults to `100`
* `ESI_MARKET_HISTORY_RATE_LIMIT_PER_S` - The number of requests allowed per second for market history - defaults to `5` - **increasing this could get you banned**
* `ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S` - The number of requests allowed per second for character corporation history - defaults to `5` - **increasing this could get you banned**
* `LOG_LEVEL` - How much logging to do - defaults to `INFO` - options are `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `FATAL`
* `EVE_OAUTH_CLIENT_ID` - The client ID of the application you created on the EVE Developer Portal
* `EVE_OAUTH_SECRET_KEY` - The secret key of the application you created on the EVE Developer Portal
* `EVE_OAUTH_CALLBACK_URL` - The callback URL of the application you created on the EVE Developer Portal - defaults to `http://localhost:8182/esiproxy/login/callback`
* `EVE_OAUTH_AUTHORIZATION_URL` - defaults to `https://login.eveonline.com/v2/oauth/authorize`
* `EVE_OAUTH_TOKEN_URL` - defaults to `https://login.eveonline.com/v2/oauth/token`
* `HTTP_CONNECT_TIMEOUT` - defaults to `PT5S`
* `HTTP_READ_TIMEOUT` - defaults to `PT60S`
* `HTTP_WRITE_TIMEOUT` - defaults to `PT60S`
* `HTTP_CALL_TIMEOUT` - defaults to `PT60S`

## Character login
To login with a character, open the proxy interface at http://localhost:8182/ and click "Log in".
After a successful login, you'll be shown a "Proxy key".
Use this key in-place of your normal OAuth access token, like:
```bash
curl "http://localhost:8182/latest/characters/{character_id}/blueprints/?token=<proxy key>"
```
The proxy will request an OAuth access token in the background and use that for you.

**The proxy does not send any auth data to anyone other than EVE Online.**
Refresh tokens are stored unencrypted on disk in a Docker volume.

### Custom ESI application
If you want the proxy to use your own client ID and secret, you can configure them via `EVE_OAUTH_CLIENT_ID` and `EVE_OAUTH_SECRET_KEY`.
You will have to do this if you want to use a port other than 8182.

Create an application in the the [EVE Developer Portal](https://developers.eveonline.com/):
* Select "Authentication & API Access"
* Select whatever scopes you want to use
* Set the callback URL to `http://localhost:8182/esiproxy/login/callback` - be sure to change the port number if you're not using the default

Copy the "Client ID" and "Secret Key" and set them on the `EVE_OAUTH_CLIENT_ID` and `EVE_OAUTH_SECRET_KEY` environment variables respectively.
For instance:
```bash
docker run -e "EVE_OAUTH_CLIENT_ID=<your client id>" -e "EVE_OAUTH_CLIENT_SECRET=<your secret key>"
```

## Overhead
The EVE ESI Proxy is built on [Helidon](https://helidon.io/), a fast HTTP server stack for Java 21,
and [OkHttp](https://square.github.io/okhttp/), a fast HTTP client for Java.
It'll easily handle tens of thousands of requests per second without breaking a sweat, way more than you'd ever need.
See [this ticket](https://github.com/autonomouslogic/eve-esi-proxy/issues/23) for some very basic load testing.

## Resources
* [Docker repository](https://hub.docker.com/r/autonomouslogic/eve-esi-proxy)
* [ESI Docs](https://docs.esi.evetech.net/)
* [ESI API](https://esi.evetech.net/ui/)

## License
The EVE ESI Proxy itself is licensed under the [MIT-0 license](https://spdx.org/licenses/MIT-0.html).

EVE Online and all assets related to it are owned by [CCP hf.](https://www.ccpgames.com/):
* [Third-Party Developer License Agreement](https://developers.eveonline.com/license-agreement)
* [End-user License Agreement](https://community.eveonline.com/support/policies/eve-eula-en/)
