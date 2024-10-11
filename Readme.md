# EVE ESI Proxy
An HTTP proxy specifically designed for the [ESI API](https://esi.evetech.net/ui/) for [EVE Online](https://www.eveonline.com/).

_This project is currently in development.
See the [First version](https://github.com/autonomouslogic/eve-esi-proxy/milestone/1) milestone for progress.
Breaking changes may occur at any time.
Contributions welcome._

Join us on [Discord](https://everef.net/discord).

Fly safe o7

## Features
* **Cache responses** to disk to improve request times and reduce load on the ESI itself
* **Rate limiting** requests to help avoid being banned, including different limits for the endpoints which have special undocumented limits
* **User agent header** is automatically set
* _More planned, see milestone_

The ESI API is a great resource, but can be difficult to work with.
The above are all things you have to be acutely aware of.
With CCP being trigger-happy about banning IPs from accessing the API,
using this proxy will let you get on with writing your application and not worry about the minutiae of ESI lore.

## Usage
```bash
docker run -it -v eve-esi-proxy:/data -p 8182:8182 -m 512m -e "ESI_USER_AGENT=<your email>" autonomouslogic/eve-esi-proxy:latest
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
* `ESI_RATE_LIMIT_PER_S` - The number of requests allowed per second for endpoints without a special rate limit - defaults to `100`
* `ESI_MARKET_HISTORY_RATE_LIMIT_PER_S` - The number of requests allowed per second for market history - defaults to `5` - increasing this is **not recommended**
* `ESI_CHARACTER_CORPORATION_HISTORY_RATE_LIMIT_PER_S` - The number of requests allowed per second for character corporation history - defaults to `5` - increasing this is **not recommended**

## Overhead
The EVE ESI Proxy is built on [Helidon](https://helidon.io/), a fast HTTP server stack for Java 21,
and [OkHttp](https://square.github.io/okhttp/), a fast HTTP client for Java.
It'll easily handle tens of thousands of requests per second without breaking a sweat, way more than you'd ever need.
See [this ticket](https://github.com/autonomouslogic/eve-esi-proxy/issues/23) for some very basic load testing.

## License
The EVE ESI Proxy itself is licensed under the [MIT-0 license](https://spdx.org/licenses/MIT-0.html).

EVE Online and all assets related to it are owned by [CCP hf.](https://www.ccpgames.com/):
* [Third-Party Developer License Agreement](https://developers.eveonline.com/license-agreement)
* [End-user License Agreement](https://community.eveonline.com/support/policies/eve-eula-en/)

## Status

| Type         | Status                                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Code Climate | [![Maintainability](https://api.codeclimate.com/v1/badges/a71c017cbcce32d7a595/maintainability)](https://codeclimate.com/github/autonomouslogic/eve-esi-proxy/maintainability)                                                                                                                                                                                                                                        |
| Sonar Cloud  | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_eve-esi-proxy&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_eve-esi-proxy) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_eve-esi-proxy&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_eve-esi-proxy) |
| Codecov      | [![codecov](https://codecov.io/gh/autonomouslogic/eve-esi-proxy/graph/badge.svg?token=MXwjEUJRPk)](https://codecov.io/gh/autonomouslogic/eve-esi-proxy)                                                                                                                                                                                                                                                               |
