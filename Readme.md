# EVE ESI Proxy

An HTTP proxy for the EVE Online ESI API.

This project is currently in development.
See the [First version](https://github.com/autonomouslogic/esi-proxy/milestone/1) milestone for progress.

## Configuration

* `MICRONAUT_SERVER_PORT`

## Usage
```bash
docker run -it -v eve-esi-proxy:/data -p 8182:8182 autonomouslogic/esi-proxy:latest
```

## License
ESI Proxy itself and the code contained within this repo is licensed under the [MIT-0 license](https://spdx.org/licenses/MIT-0.html).

EVE Online is owned by [CCP hf.](https://www.ccpgames.com/):
* [Third-Party Developer License Agreement](https://developers.eveonline.com/license-agreement)
* [End-user License Agreement](https://community.eveonline.com/support/policies/eve-eula-en/)

## Status

| Type         | Status                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Code Climate | [![Maintainability](https://api.codeclimate.com/v1/badges/a48ec1513807fc073563/maintainability)](https://codeclimate.com/github/autonomouslogic/esi-proxy/maintainability)                                                                                                                                                                                                                            |
| Sonar Cloud  | [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_esi-proxy&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_esi-proxy) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=autonomouslogic_esi-proxy&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=autonomouslogic_esi-proxy) |
| Codecov      | [![codecov](https://codecov.io/gh/autonomouslogic/esi-proxy/graph/badge.svg?token=MXwjEUJRPk)](https://codecov.io/gh/autonomouslogic/esi-proxy)                                                                                                                                                                                                                                                       |
