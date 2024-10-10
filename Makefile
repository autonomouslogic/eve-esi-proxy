.PHONY: dist test format clean docker
ESI_PROXY_VERSION = $(shell ./gradlew properties | grep 'version:' | cut -d' ' -f 2)
DOCKER_TAG_BASE = autonomouslogic/esi-proxy
DOCKER_TAG = $(DOCKER_TAG_BASE):$(ESI_PROXY_VERSION)
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
		--build-arg "ESI_PROXY_VERSION=$(ESI_PROXY_VERSION)" \
		.

docker-push: docker
	docker push $(DOCKER_TAG)
	docker push $(DOCKER_TAG_LATEST)

docker-run: docker
	docker run -it -v eve-esi-proxy:/data -p 8182:8182 $(DOCKER_TAG)

clean:
	./gradlew clean --stacktrace

version:
	echo $(ESI_PROXY_VERSION)

renovate-validate:
	npm install renovate
	node node_modules/renovate/dist/config-validator.js
