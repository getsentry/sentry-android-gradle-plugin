.PHONY: compile all dist

compile:
	./gradlew assemble

# build distribution artifacts
dist:
	./gradlew distZip --no-parallel

all: compile dist
