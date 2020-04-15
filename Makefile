.PHONY: release clean compile dryRelease doRelease all

release: clean assemble doRelease

clean:
	./gradlew clean

compile:
	./gradlew assemble

# do a dry release (like a local deploy)
dryRelease:
	./gradlew publishToMavenLocal

# clean, build and deploy to maven central
doRelease:
	./download-sentry-cli.sh
	./gradlew uploadArchives --no-daemon

all: clean compile dryRelease
