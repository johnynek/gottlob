load("//rule:frege.bzl", "frege_library")

frege_library(name = "hello0",
              srcs = ["hello0.fr"])

frege_library(name = "hello",
              srcs = ["hello.fr"],
              deps = [":hello0"])

java_binary(name = "run_hello",
            runtime_deps = [":hello"],
            main_class = "Hello")
