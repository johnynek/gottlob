java_binary(name = "compiler",
            srcs = ["Compiler.java"],
            deps = ["@frege_library//jar", ":jar"],
            main_class = "org.bykn.gottlob.Compiler",
            visibility = ["//visibility:public"],
            )


java_library(name = "jar",
  srcs = ["JarCreator.java", "JarHelper.java"],
  visibility = ["//visibility:public"],
)


java_binary(name = "binary",
  runtime_deps = [
    ":jar",
  ],
  main_class = "org.bykn.gottlob.JarCreator",
  visibility = ["//visibility:public"],
)
