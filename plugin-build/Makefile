.PHONY: release clean compile dryRelease doRelease all

release: clean compile doRelease

clean:
	./gradlew clean

compile:
	./gradlew assemble

# build distribution artifacts
dist:
	./gradlew distZip

# do a dry release (like a local deploy)
dryRelease:
	./gradlew publishToMavenLocal --no-parallel

# clean, build and deploy to maven central
doRelease:
	./gradlew publishMavenPublicationToMavenCentralRepository --no-parallel
	./gradlew publishSentryPluginPluginMarkerMavenPublicationToMavenCentralRepository --no-parallel
	./gradlew closeAndReleaseRepository

all: clean compile dist dryRelease
