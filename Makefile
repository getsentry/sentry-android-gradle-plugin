.PHONY: format clean preMerge stop all

format:
	./gradlew ktlintFormat

clean:
	./gradlew clean

preMerge:
	./gradlew :sentry-kotlin-compiler-plugin:jacocoTestReport

# We stop gradle at the end to make sure the cache folders
# don't contain any lock files and are free to be cached.
stop:
	./gradlew --stop

all: stop clean format preMerge
