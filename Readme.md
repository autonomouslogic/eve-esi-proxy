# EVE ESI Proxy
An HTTP proxy specifically designed for the [ESI API](https://esi.evetech.net/ui/) for [EVE Online](https://www.eveonline.com/).

_This project is currently in development.
See the [First version](https://github.com/autonomouslogic/eve-esi-proxy/milestone/1) milestone for progress.
Contributions welcome._

## Features
* **Cache responses** to disk to improve request times and reduce load on the ESI itself
* _More planned, see milestone_

These are all things you have to do when working with the ESI to not risk getting banned.
Using this proxy means you can get on with writing your application and not worry about the minutiae of ESI lore.

## Usage
The proxy is easily run from Docker:
```bash
docker run -it -p 8182:8182 autonomouslogic/esi-proxy:latest
```

Then you request data as you would on the ESI, just from localhost instead:
```bash
curl http://localhost:8182/latest/status/
```

## Overhead
The EVE ESI Proxy is built on [Helidon](https://helidon.io/), a fast HTTP stack for Java 21.
It'll easily handle tens of thousands of requests per second without breaking a sweat, way more than you'd ever need.
See [this ticket](https://github.com/autonomouslogic/eve-esi-proxy/issues/23) for some very basic load testing.

## License
The EVE ESI Proxy itself and the code contained within this repo is created
Kenn, aka [Dato Tovikov](https://evewho.com/character/1452072530) of [EVE Ref](https://everef.net/).
It is licensed under the [MIT-0 license](https://spdx.org/licenses/MIT-0.html).

EVE Online and all assets related to it are owned by [CCP hf.](https://www.ccpgames.com/):
* [Third-Party Developer License Agreement](https://developers.eveonline.com/license-agreement)
* [End-user License Agreement](https://community.eveonline.com/support/policies/eve-eula-en/)

## Status

| Type         | Status                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Code Climate | [![Maintainability](https://api.codeclimate.com/v1/badges/a48ec1513807fc073563/maintainability)](https://codeclimate.com/github/autonomouslogic/esi-proxy/maintainability)                                                                                                                                                                                                                            |
| Sonar Cloud  | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_esi-proxy&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_esi-proxy) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_esi-proxy&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_esi-proxy) |
| Codecov      | [![codecov](https://codecov.io/gh/autonomouslogic/esi-proxy/graph/badge.svg?token=MXwjEUJRPk)](https://codecov.io/gh/autonomouslogic/esi-proxy)                                                                                                                                                                                                                                                       |
