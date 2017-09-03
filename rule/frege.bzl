

def _frege_impl(ctx):
  src_files = [f.path for f in ctx.files.srcs]

  compile_jars = depset()
  runtime_jars = depset()

  for dep_target in ctx.attr.deps:
    if java_common.provider in dep_target:
        java_provider = dep_target[java_common.provider]
        compile_jars += java_provider.compile_jars
        # todo: frege needs the full jars for now. It seems the default ijar removes signatures
        # needed
        runtime_jars += java_provider.transitive_runtime_jars
    else:
      fail("expected a java provider but missing in: %s" % dep_target)

  ins = runtime_jars + list(ctx.files.srcs)

  srcjar = ctx.new_file(ctx.outputs.jar, "%s_compiled_frege.srcjar" % ctx.label.name)

  if len(compile_jars) > 0:
    classpath = ["-fp", (":".join([jar.path for jar in runtime_jars.to_list()]))]
  else:
    classpath = []

  ctx.action(
      inputs = ins,
      outputs = [srcjar],
      executable = ctx.executable._frege_compiler,
      mnemonic = "Fregec",
      progress_message = "fregec %s (%s files)" % (ctx.label, len(ins)),
      arguments = ["--jvm_flag=-Xss2m"] + classpath + [srcjar.path] + [f.path for f in ctx.files.srcs]
      )

  provider = java_common.compile(
        ctx,
        source_jars = [srcjar],
        source_files = [],
        output = ctx.outputs.jar,
        javac_opts = [], # ctx.attr.javacopts + ctx.attr.javac_jvm_flags,
        deps = [dep[java_common.provider] for dep in ctx.attr.deps] + [ctx.attr._frege_lib[java_common.provider]],
        exports = [],
        java_toolchain = ctx.attr._java_toolchain,
        host_javabase = ctx.attr._host_javabase,
    )

  return struct(providers = [provider])

frege_library = rule(
    implementation = _frege_impl,
    attrs = {
        "srcs": attr.label_list(mandatory=False, allow_files=FileType([".fr"])),
        "deps": attr.label_list(),
        "_frege_compiler": attr.label(executable=True, cfg="host", default=Label("//src/java/org/bykn/gottlob:compiler"), allow_files=True),
        "_frege_lib": attr.label(default=Label("@frege_library//jar")),
        "_java_toolchain": attr.label(default = Label("@bazel_tools//tools/jdk:toolchain")),
        "_host_javabase": attr.label(default = Label("//tools/defaults:jdk"))
    },
    outputs = {
      "jar": "%{name}.jar",
    },
    fragments = ["java"]
)

# "@frege_library//jar gets the jar
def frege_repositories():
    native.http_jar(
      name = "frege_library",
      url = "https://github.com/Frege/frege/releases/download/3.24alpha/frege3.24.100.jar",
      sha256 = "6ad1c4535d61b1f0cd9edbbe46bdad110cfac15d413bcc28dcbb78cd8800e6e9",
    )
