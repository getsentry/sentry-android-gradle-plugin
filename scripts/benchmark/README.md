# Benchmark script

This is a convenient script that benchmarks SAGP performance against the [duckduckgo-android](https://github.com/duckduckgo/Android) app.

The script does the following:

* Checks out the `duckduckgo` app locally
* Runs [gradle-profiler](https://github.com/gradle/gradle-profiler) with predefined [scenarios](/scripts/benchmark/duckduckgo/duckduckgo.scenarios)
* Applies git `.patch` which, in turn, applies sentry-android-gradle-plugin and sentry-android sdk to the duckduckgo app
* Runs gradle-profiler with the same scenarios to measure the performance overhead of the Sentry gradle plugin
* Saves the results under `/results/pre-sentry` and `/results/post-sentry`

After launching the script, get some coffee and enjoy your time Slacking for the next 30 minutes or so :)
Or you can cook some meat using your laptop as frying pan ;)
