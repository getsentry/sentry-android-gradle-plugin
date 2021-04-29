.PHONY: release clean compile dryRelease doRelease all

release: clean compile doRelease

clean:
	./gradlew clean

compile:
	./gradlew assemble

# do a dry release (like a local deploy)
dryRelease:
	./gradlew publishToMavenLocal

# clean, build and deploy to maven central
doRelease:
	./gradlew publishMavenPublicationToMavenCentralRepository
	./gradlew publishSentryPluginPluginMarkerMavenPublicationToMavenCentralRepository
	./gradlew closeAndReleaseRepository

all: clean compile dryRelease
