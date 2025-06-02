EVE ESI Proxy Changelog

## [1.0.11](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.10...1.0.11) (2025-06-02)


### Bug Fixes

* **deps:** update all non-major dependencies ([#115](https://github.com/autonomouslogic/eve-esi-proxy/issues/115)) ([5ba18ac](https://github.com/autonomouslogic/eve-esi-proxy/commit/5ba18ac970de8bce2172e33da6b8d38b31b9ff83))
* **deps:** update dependency com.autonomouslogic.commons:commons-java to v1.9.7 [skip release] ([#111](https://github.com/autonomouslogic/eve-esi-proxy/issues/111)) ([083096b](https://github.com/autonomouslogic/eve-esi-proxy/commit/083096b171caf11a40bf1647bcc1ca7d96898333))

## [1.0.10](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.9...1.0.10) (2025-05-05)


### Bug Fixes

* **deps:** update all non-major dependencies ([#110](https://github.com/autonomouslogic/eve-esi-proxy/issues/110)) ([3043a63](https://github.com/autonomouslogic/eve-esi-proxy/commit/3043a63697accdb1ead487f77f4c6d79b66d743b))

## [1.0.9](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.8...1.0.9) (2025-04-15)


### Dependency Updates

* **deps:** update plugin org.danilopianini.git-sensitive-semantic-versioning-gradle-plugin to v5 ([#109](https://github.com/autonomouslogic/eve-esi-proxy/issues/109)) ([d8dac1b](https://github.com/autonomouslogic/eve-esi-proxy/commit/d8dac1ba21a18ca09b2f0df2685795be641b8625))

## [1.0.8](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.7...1.0.8) (2025-04-07)


### Bug Fixes

* **deps:** update all non-major dependencies ([#108](https://github.com/autonomouslogic/eve-esi-proxy/issues/108)) ([49b50a1](https://github.com/autonomouslogic/eve-esi-proxy/commit/49b50a15a35e36ac214791b7e68ece4dc15ad6fd))

## [1.0.7](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.6...1.0.7) (2025-04-01)


### Bug Fixes

* Close HTTP responses ([#104](https://github.com/autonomouslogic/eve-esi-proxy/issues/104)) ([0e28ba2](https://github.com/autonomouslogic/eve-esi-proxy/commit/0e28ba24219451e1ce07cd11651988a0e8801b3a))
* Fixed logging of rate limits ([0ce0e9f](https://github.com/autonomouslogic/eve-esi-proxy/commit/0ce0e9fbc769cad533a460bdd5da05cca4b524a8))
* Switched to OkHttp's async API ([#105](https://github.com/autonomouslogic/eve-esi-proxy/issues/105)) ([9d25a97](https://github.com/autonomouslogic/eve-esi-proxy/commit/9d25a97071997ebad1caa5de36d2525af8069078))


### Continuous Integration

* Re-enabled releases ([961d527](https://github.com/autonomouslogic/eve-esi-proxy/commit/961d527037413fe46f7d73656512cab7bc24b101))

## [1.0.6](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.5...1.0.6) (2025-03-04)


### Bug Fixes

* **deps:** update all non-major dependencies ([#102](https://github.com/autonomouslogic/eve-esi-proxy/issues/102)) ([22c4c3f](https://github.com/autonomouslogic/eve-esi-proxy/commit/22c4c3f20720dc2a884105c8a3cdad3d92b93068))

## [1.0.5](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.4...1.0.5) (2025-02-04)


### Documentation

* Note on handling multiple pages ([1dc9865](https://github.com/autonomouslogic/eve-esi-proxy/commit/1dc98659d4f1c8bd6402102eb11144070053ee97))


### Dependency Updates

* **deps:** update plugin io.freefair.lombok to v8.12.1 ([#101](https://github.com/autonomouslogic/eve-esi-proxy/issues/101)) ([7a021ce](https://github.com/autonomouslogic/eve-esi-proxy/commit/7a021ce103256bb5ea32ab13a9274aee56fb9513))


### Continuous Integration

* Disbled semantic PR ([0833780](https://github.com/autonomouslogic/eve-esi-proxy/commit/0833780270b79ed1a3e8cf3481af85010dbc8f70))

## [1.0.4](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.3...1.0.4) (2025-02-03)


### Bug Fixes

* **deps:** update all non-major dependencies ([#99](https://github.com/autonomouslogic/eve-esi-proxy/issues/99)) ([bad7d60](https://github.com/autonomouslogic/eve-esi-proxy/commit/bad7d605cb549e3c9f9f13a5a3fc6260beb17478))
* Minor tweaks ([#98](https://github.com/autonomouslogic/eve-esi-proxy/issues/98)) ([9543868](https://github.com/autonomouslogic/eve-esi-proxy/commit/954386856b80410e7db36639445ee0dd2a4e261b))


### Dependency Updates

* **deps:** update plugin org.danilopianini.git-sensitive-semantic-versioning-gradle-plugin to v4 ([#97](https://github.com/autonomouslogic/eve-esi-proxy/issues/97)) ([c79957d](https://github.com/autonomouslogic/eve-esi-proxy/commit/c79957d0c5789fddb0b0c08c6064db95a674b93b))

## [1.0.3](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.2...1.0.3) (2025-01-13)


### Bug Fixes

* Default ESI base URL shouldn't have a trailing slash ([098feae](https://github.com/autonomouslogic/eve-esi-proxy/commit/098feaec844b2afcdfd9ae5d897dd9849346e0e4))
* Double slashes in the beginning of the path should not switch host ([#95](https://github.com/autonomouslogic/eve-esi-proxy/issues/95)) ([4866dfb](https://github.com/autonomouslogic/eve-esi-proxy/commit/4866dfbb81b40d20b7135ab1b4d7a98c5b6fe5d1))
* Handle compressed requests and responses ([#96](https://github.com/autonomouslogic/eve-esi-proxy/issues/96)) ([f0f6ecd](https://github.com/autonomouslogic/eve-esi-proxy/commit/f0f6ecdf23d154c2c82827b343de3d771f247cfc))


### Dependency Updates

* **deps:** update plugin com.diffplug.spotless to v7 ([#92](https://github.com/autonomouslogic/eve-esi-proxy/issues/92)) ([d71aff0](https://github.com/autonomouslogic/eve-esi-proxy/commit/d71aff0b4f78b919413b174cec2d6bd547a0a78b))

## [1.0.2](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.1...1.0.2) (2025-01-07)


### Bug Fixes

* **deps:** update all non-major dependencies ([#91](https://github.com/autonomouslogic/eve-esi-proxy/issues/91)) ([b18308d](https://github.com/autonomouslogic/eve-esi-proxy/commit/b18308d8324fd0d251df5a5d3b9a0cb4326f326b))

## [1.0.1](https://github.com/autonomouslogic/eve-esi-proxy/compare/1.0.0...1.0.1) (2025-01-06)


### Bug Fixes

* **deps:** update all non-major dependencies ([#90](https://github.com/autonomouslogic/eve-esi-proxy/issues/90)) ([3e4e2ac](https://github.com/autonomouslogic/eve-esi-proxy/commit/3e4e2ac455f2e1b6e24b01d8b12e66f81349c9a8))

## 1.0.0 (2024-12-20)


### Features

* Automatic retries for 5xx errors ([#79](https://github.com/autonomouslogic/eve-esi-proxy/issues/79)) ([550bdb9](https://github.com/autonomouslogic/eve-esi-proxy/commit/550bdb9ccb034aa2ad886a95215a70945c5340dc))
* Automatically fetch all pages and merge responses ([#54](https://github.com/autonomouslogic/eve-esi-proxy/issues/54)) ([d0d70cd](https://github.com/autonomouslogic/eve-esi-proxy/commit/d0d70cd628d5eb70bcbd5074f645e9a20a562b1e))
* Basic caching ([#20](https://github.com/autonomouslogic/eve-esi-proxy/issues/20)) ([4ced42f](https://github.com/autonomouslogic/eve-esi-proxy/commit/4ced42f1859cf1b2e8e750bcf1e192bd0a24edc7))
* Basic relay ([#14](https://github.com/autonomouslogic/eve-esi-proxy/issues/14)) ([73e030f](https://github.com/autonomouslogic/eve-esi-proxy/commit/73e030f891aecd9faf3be7ca8db4e212ac113458))
* Basic setup ([#1](https://github.com/autonomouslogic/eve-esi-proxy/issues/1)) ([0ba5b51](https://github.com/autonomouslogic/eve-esi-proxy/commit/0ba5b51c9cb3e5c81c5213b097ce088bb49d52fa))
* Cache status header ([#27](https://github.com/autonomouslogic/eve-esi-proxy/issues/27)) ([19f5950](https://github.com/autonomouslogic/eve-esi-proxy/commit/19f595063ad69cd2cdae85ed5ce5b52450ecf2d6))
* Fetch pages concurrently ([#81](https://github.com/autonomouslogic/eve-esi-proxy/issues/81)) ([d863abb](https://github.com/autonomouslogic/eve-esi-proxy/commit/d863abb25b46a1b2c249fc43e2b799707de5afc2))
* Handle error limit headers ([#58](https://github.com/autonomouslogic/eve-esi-proxy/issues/58)) ([2a80077](https://github.com/autonomouslogic/eve-esi-proxy/commit/2a8007706c4c06b0d7c939524f2dff93ddd02130))
* Handle OAuth and character login ([#60](https://github.com/autonomouslogic/eve-esi-proxy/issues/60)) ([5a30ea3](https://github.com/autonomouslogic/eve-esi-proxy/commit/5a30ea3d21deca0197b7d41e0b5e2bf93f536666))
* Handle user agent ([#51](https://github.com/autonomouslogic/eve-esi-proxy/issues/51)) ([87ff746](https://github.com/autonomouslogic/eve-esi-proxy/commit/87ff746ef48a99612b9e8dcbfc03bc567f38aa8f))
* Increased HTTP timeouts and made them configurable ([#78](https://github.com/autonomouslogic/eve-esi-proxy/issues/78)) ([8b99ad6](https://github.com/autonomouslogic/eve-esi-proxy/commit/8b99ad6cfe9c65b72464481f80be8072edadfd62))
* Proxy PUT, POST, and DELETE requests ([#59](https://github.com/autonomouslogic/eve-esi-proxy/issues/59)) ([0d88a7e](https://github.com/autonomouslogic/eve-esi-proxy/commit/0d88a7e5b8d8f4f0c30bcf90915be34f53d16dff))
* Rate limiting ([#52](https://github.com/autonomouslogic/eve-esi-proxy/issues/52)) ([5ab85a5](https://github.com/autonomouslogic/eve-esi-proxy/commit/5ab85a5e80ddf28e52be57e30acede947efa1ae5))
* Redirect all URL paths, not just the root ([#16](https://github.com/autonomouslogic/eve-esi-proxy/issues/16)) ([0d5f869](https://github.com/autonomouslogic/eve-esi-proxy/commit/0d5f86950842c05fe53da483af019a7491f320b8))
* Select scopes on login ([#85](https://github.com/autonomouslogic/eve-esi-proxy/issues/85)) ([ee24af1](https://github.com/autonomouslogic/eve-esi-proxy/commit/ee24af1e5cd0227f54eb9bcd929ce516d4edfb10))
* Simple UI with Thymeleaf ([#75](https://github.com/autonomouslogic/eve-esi-proxy/issues/75)) ([3237237](https://github.com/autonomouslogic/eve-esi-proxy/commit/3237237d1c801b5efef5fd58ed792c1781e4d59d))
* Support for HEAD and OPTIONS requests ([#77](https://github.com/autonomouslogic/eve-esi-proxy/issues/77)) ([fc66929](https://github.com/autonomouslogic/eve-esi-proxy/commit/fc669295c0f3574e273fadf34742dbf363e17419))


### Bug Fixes

* **deps:** update all non-major dependencies ([#35](https://github.com/autonomouslogic/eve-esi-proxy/issues/35)) ([c7f84f2](https://github.com/autonomouslogic/eve-esi-proxy/commit/c7f84f273666312664d07e7daf32285df256430c))
* **deps:** update all non-major dependencies ([#39](https://github.com/autonomouslogic/eve-esi-proxy/issues/39)) ([9555bdd](https://github.com/autonomouslogic/eve-esi-proxy/commit/9555bdd2b3ae7917ac6e6f8086d7aaa9bc705cab))
* **deps:** update dependency com.autonomouslogic.commons:commons-java to v1.9.2 ([#87](https://github.com/autonomouslogic/eve-esi-proxy/issues/87)) ([f7ba15b](https://github.com/autonomouslogic/eve-esi-proxy/commit/f7ba15b4c7835b31cad7bc490b1e393e3fe00601))
* Fixed HTTP cache dir config in Dockerfile ([d1ba5bb](https://github.com/autonomouslogic/eve-esi-proxy/commit/d1ba5bb6934589399a72c77f419fd281b0846430))
* Fixed version file ([6692ab2](https://github.com/autonomouslogic/eve-esi-proxy/commit/6692ab263d0dd5c181ace4f1ac87737a85d0c427))
* JVM args ([7f7c4e0](https://github.com/autonomouslogic/eve-esi-proxy/commit/7f7c4e061010be063688d8ed0ce60719bec856a9))
* Limited scopes to 66 ([f7efdad](https://github.com/autonomouslogic/eve-esi-proxy/commit/f7efdad034a5b326efa5be831828078844c9e917))
* Sample curl command on character page ([#76](https://github.com/autonomouslogic/eve-esi-proxy/issues/76)) ([4af0802](https://github.com/autonomouslogic/eve-esi-proxy/commit/4af08021375106a089b894f449a262824967ee8e))
* Simpler configs ([#50](https://github.com/autonomouslogic/eve-esi-proxy/issues/50)) ([a160f17](https://github.com/autonomouslogic/eve-esi-proxy/commit/a160f17a2f6b2bc4ed63eaa5f1dad0a3715485d2))
* UI with sans-serif ([0fe93c3](https://github.com/autonomouslogic/eve-esi-proxy/commit/0fe93c3e210fec5b084896388a652f0f6b40fc6d))
* Updated titles in the UI ([b886ed1](https://github.com/autonomouslogic/eve-esi-proxy/commit/b886ed188644744cdd61c5a303be2e3c0bb8f2f4))


### Documentation

* Better readme ([#46](https://github.com/autonomouslogic/eve-esi-proxy/issues/46)) ([fa2391e](https://github.com/autonomouslogic/eve-esi-proxy/commit/fa2391ec7e3eebd3d7fe2e06cebde411775fd6c0))
* Better readme ([#82](https://github.com/autonomouslogic/eve-esi-proxy/issues/82)) ([9273bdf](https://github.com/autonomouslogic/eve-esi-proxy/commit/9273bdffe28c6d776f6badf3e0f1b96afa8ee61c))
* License and code metrics ([81530f9](https://github.com/autonomouslogic/eve-esi-proxy/commit/81530f950704efd99fa4eb1e83446462ac73283e))
* Links in readme ([91aef90](https://github.com/autonomouslogic/eve-esi-proxy/commit/91aef901fe6f088a34bab49fd52716c83c280de2))
* Note on development ([dadb6ee](https://github.com/autonomouslogic/eve-esi-proxy/commit/dadb6ee1da542fea42e245a6de286f243774bbde))
* Similar projects ([#80](https://github.com/autonomouslogic/eve-esi-proxy/issues/80)) ([210b7e9](https://github.com/autonomouslogic/eve-esi-proxy/commit/210b7e96fe0183c875dbc9cb8d64f9b63ad06eec))
* Updated badges ([513a725](https://github.com/autonomouslogic/eve-esi-proxy/commit/513a7255af765c6d698550e6fe800bb40b83a4d2))


### Dependency Updates

* **deps:** update all non-major dependencies ([#32](https://github.com/autonomouslogic/eve-esi-proxy/issues/32)) ([fa8ae80](https://github.com/autonomouslogic/eve-esi-proxy/commit/fa8ae80ba776a1be42fd7e8b8dbc7233f5baab95))
* **deps:** update all non-major dependencies ([#33](https://github.com/autonomouslogic/eve-esi-proxy/issues/33)) ([88b917a](https://github.com/autonomouslogic/eve-esi-proxy/commit/88b917a3108231bb886d7e999818aa10b6f36e4d))
* **deps:** update all non-major dependencies ([#38](https://github.com/autonomouslogic/eve-esi-proxy/issues/38)) ([5d7179a](https://github.com/autonomouslogic/eve-esi-proxy/commit/5d7179a963586f26e16fd32697cb2a119086a14f))
* **deps:** update all non-major dependencies ([#40](https://github.com/autonomouslogic/eve-esi-proxy/issues/40)) ([e2c0d32](https://github.com/autonomouslogic/eve-esi-proxy/commit/e2c0d32255a71422a66f19a23616488c767f224c))
* **deps:** update all non-major dependencies ([#41](https://github.com/autonomouslogic/eve-esi-proxy/issues/41)) ([8e26c5a](https://github.com/autonomouslogic/eve-esi-proxy/commit/8e26c5a0e1f3af0d08ecc16d5b5edd405bd740b4))
* **deps:** update all non-major dependencies ([#86](https://github.com/autonomouslogic/eve-esi-proxy/issues/86)) ([17a3a44](https://github.com/autonomouslogic/eve-esi-proxy/commit/17a3a44bc1027a48c5ff41b9e5cc15a61fc9e8d1))
* **deps:** update all non-major dependencies ([#89](https://github.com/autonomouslogic/eve-esi-proxy/issues/89)) ([b3ecf81](https://github.com/autonomouslogic/eve-esi-proxy/commit/b3ecf8103b98dfa54cbedf9676ab4102d153c849))
* **deps:** update codecov/codecov-action action to v4 ([#29](https://github.com/autonomouslogic/eve-esi-proxy/issues/29)) ([bff7170](https://github.com/autonomouslogic/eve-esi-proxy/commit/bff71703ec19b24a3a1f2be6e6e46fa0653d58ef))
* **deps:** update codecov/codecov-action action to v5 ([#88](https://github.com/autonomouslogic/eve-esi-proxy/issues/88)) ([92d4f81](https://github.com/autonomouslogic/eve-esi-proxy/commit/92d4f812dbc8a12817024dd49f6e41e8c4b58f47))
* **deps:** update plugin org.danilopianini.git-sensitive-semantic-versioning-gradle-plugin to v3 ([#30](https://github.com/autonomouslogic/eve-esi-proxy/issues/30)) ([a71e8cd](https://github.com/autonomouslogic/eve-esi-proxy/commit/a71e8cd7cd41204461f3a887973a17293e69f0ab))
* **deps:** update ubuntu docker tag to v24 ([#34](https://github.com/autonomouslogic/eve-esi-proxy/issues/34)) ([13930a0](https://github.com/autonomouslogic/eve-esi-proxy/commit/13930a0fc9c6a43b22110cd2dbbbf507ffdd7149))


### Miscellaneous Chores

* Better logging on HTTP cache dir setup ([f91606c](https://github.com/autonomouslogic/eve-esi-proxy/commit/f91606c3b510462d3f0152cb7d2a2fb7b8f1c4be))
* Configure Renovate ([#28](https://github.com/autonomouslogic/eve-esi-proxy/issues/28)) ([fc639c6](https://github.com/autonomouslogic/eve-esi-proxy/commit/fc639c6a441801fa2a0082c7ff83e347c2e56306))
* Git ignore update ([fba82c1](https://github.com/autonomouslogic/eve-esi-proxy/commit/fba82c17ca55fc129388d508117b0249d9d03cdc))
* Increased recommended memory ([f5e9ad9](https://github.com/autonomouslogic/eve-esi-proxy/commit/f5e9ad98d3265f262b2bbe286f20a41fee9ad5bd))
* Initial commit ([6bacb73](https://github.com/autonomouslogic/eve-esi-proxy/commit/6bacb7317b4573a793ca2e9506be17d096e6d81d))
* Renamed to eve-esi-proxy ([397217e](https://github.com/autonomouslogic/eve-esi-proxy/commit/397217e4dd77d5757f40e659acc48b5cd8c1192f))


### Code Refactoring

* Added version number to the build ([#49](https://github.com/autonomouslogic/eve-esi-proxy/issues/49)) ([e96ad2e](https://github.com/autonomouslogic/eve-esi-proxy/commit/e96ad2e68001c66004cb3cf71e3553053a4790c6))
* Converted all configs to the same standard ([#47](https://github.com/autonomouslogic/eve-esi-proxy/issues/47)) ([67b4e7c](https://github.com/autonomouslogic/eve-esi-proxy/commit/67b4e7c443136a0f763fac8b9acbddcc951fb52f))
* Moved some classes around ([5a7d825](https://github.com/autonomouslogic/eve-esi-proxy/commit/5a7d8250cec56ea5a0ab6dc23d7d5c78b8860f6a))
* Moved user-agent handling to an interceptor ([#53](https://github.com/autonomouslogic/eve-esi-proxy/issues/53)) ([250cf00](https://github.com/autonomouslogic/eve-esi-proxy/commit/250cf00f221cc94dc411d6170349718552171b22))
* Renamed base package ([7160b51](https://github.com/autonomouslogic/eve-esi-proxy/commit/7160b51b1b2253e34f3ad4b487f0b59a5beca263))
* Renamed project ([#48](https://github.com/autonomouslogic/eve-esi-proxy/issues/48)) ([2180b20](https://github.com/autonomouslogic/eve-esi-proxy/commit/2180b2049a8d2a502bf85e873bcbdca2e29a1fb2))
* Switched to Helidon Nina ([#37](https://github.com/autonomouslogic/eve-esi-proxy/issues/37)) ([befe1e2](https://github.com/autonomouslogic/eve-esi-proxy/commit/befe1e2a28b7eabaa2bad40270bb119ef6621dc9))


### Tests

* Added testing for caching of 4xx and 5xx responses ([37d347d](https://github.com/autonomouslogic/eve-esi-proxy/commit/37d347da2be0449c591ebe5d07170f44d9fb0fa4))
* More architecture testing ([90be7a0](https://github.com/autonomouslogic/eve-esi-proxy/commit/90be7a011048dd0c57de1c6d3d035e63f74519d7))
* Testing for conditional cache requests ([#43](https://github.com/autonomouslogic/eve-esi-proxy/issues/43)) ([59521ed](https://github.com/autonomouslogic/eve-esi-proxy/commit/59521eddaf39f9cadc1e678ed94b4abf4dd503b7))


### Continuous Integration

* Automatic releases ([609a356](https://github.com/autonomouslogic/eve-esi-proxy/commit/609a3560f9e035b73ccd588b5c10928ef36e35d7))
