.PHONY: format preMerge all

format:
	./gradlew spotlessApply

preMerge:
	./gradlew preMerge --continue

all: format preMerge
