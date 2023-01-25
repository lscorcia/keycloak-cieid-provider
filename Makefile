SERVICE_TARGET := maven:3.8.7-amazoncorretto-11

# all our targets are phony (no files to check).
.PHONY: help package

# suppress makes own output
#.SILENT:

help:
	@echo 'Usage: make [TARGET]                                             '

build: package

package:
	docker run --rm -v ${shell pwd}:/opt/app -w /opt/app ${SERVICE_TARGET} bash -c "mvn clean package"