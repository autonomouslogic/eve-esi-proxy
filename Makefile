.PHONY: dist test format clean docker
EVE_ESI_PROXY_VERSION = $(shell ./gradlew properties | grep 'version:' | cut -d' ' -f 2)
DOCKER_TAG_BASE = autonomouslogic/eve-esi-proxy
DOCKER_TAG = $(DOCKER_TAG_BASE):$(EVE_ESI_PROXY_VERSION)
DOCKER_TAG_LATEST = $(DOCKER_TAG_BASE):latest

dist:
	./gradlew distTar --stacktrace

test:
	./gradlew test --stacktrace

lint:
	./gradlew spotlessCheck --stacktrace

format:
	./gradlew spotlessApply --stacktrace

docker: dist
	docker build \
		-f docker/Dockerfile \
		--tag $(DOCKER_TAG) \
		--tag $(DOCKER_TAG_LATEST) \
		--build-arg "EVE_ESI_PROXY_VERSION=$(EVE_ESI_PROXY_VERSION)" \
		.

docker-push: docker
	docker push $(DOCKER_TAG)
	docker push $(DOCKER_TAG_LATEST)

docker-run: docker
	docker run -p 8182:8182 -it $(DOCKER_TAG)

clean:
	./gradlew clean --stacktrace

version:
	echo $(ESI_PROXY_VERSION)

renovate-validate:
	npm install renovate
	node node_modules/renovate/dist/config-validator.js
