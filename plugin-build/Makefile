.PHONY: clean compile all dist

clean:
	./gradlew clean

compile:
	./gradlew assemble

# build distribution artifacts
dist:
	./gradlew distZip --no-daemon

all: clean compile dist
