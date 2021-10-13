.PHONY: format clean preMerge all

format:
	./gradlew ktlintFormat

clean:
	./gradlew clean

preMerge:
	./gradlew preMerge --continue

# We stop gradle at the end to make sure the cache folders
# don't contain any lock files and are free to be cached.
stop:
	./gradlew --stop

all: stop clean ktlintFormat preMerge
