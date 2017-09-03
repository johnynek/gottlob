# gottlob
Bazel rules for the [Frege programming language](https://github.com/Frege/frege/)

These are rules that use some recent bazel features to allow clean integration of calling frege from any
language that supports Bazel's java_provider, and vice-versa.

This also may serve as an example of bazel support for a language that compiles via Java.

# Getting started

See the example in [test/BUILD](test/BUILD) or check out this repo and run:
```
bazel run test:run_hello foo bar baz
```
