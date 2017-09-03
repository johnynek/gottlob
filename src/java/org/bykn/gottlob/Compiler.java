package org.bykn.gottlob;

import frege.compiler.Main;
import frege.prelude.PreludeArrays;
import frege.prelude.PreludeBase;
import frege.run8.Thunk;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;

class Compiler {

  static void deletePath(Path p) throws IOException {
    Files.walk(p, FileVisitOption.FOLLOW_LINKS)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }

  public static void main(String[] args) throws Exception {
    // for(String a: args) {
    //   System.out.println(a);
    // }
    int idx = 0;
    String classpath = null;
    if (args[idx].equals("-fp")) {
      classpath = args[idx + 1];
      idx += 2;
    }
    String outputJar = args[idx];
    idx += 1;
    // the rest of the args are src files

    Path tmpPath = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "tmp");
    String fregeOutputDir = tmpPath.toString();
    // args:
    // -d fregeOutputDir -j -make <files>

    ArrayList<String> argList = new ArrayList<>();
    argList.add("-d");
    argList.add(fregeOutputDir);
    if (classpath != null) {
      argList.add("-fp");
      argList.add(classpath);
    }
    argList.add("-j");
    argList.add("-make");
    for(int i = idx; i < args.length; i += 1) {
      argList.add(args[i]);
    }
    String[] fregeArgs = argList.toArray(new String[0]);
    // System.out.println("about to Main");
    // for(String arg: fregeArgs) {
    //   System.out.println(arg);
    // }
    try {
      /*
       * Just calling Main.main causes the jvm to exit it seems, so
       * we have to jump through these hoops
       */
      frege.run.RunTM.argv = fregeArgs;

        PreludeBase.TST.<Boolean>performUnsafe(Main.$main
               (Thunk.lazy(PreludeArrays.IListSource_JArray.<String/*<Character>*/>toList(fregeArgs)))
          ).call();
      System.out.println("compiled");
      /**
       * Now build the output jar
       */
      String[] jarCreatorArgs = {
        outputJar,
        fregeOutputDir
      };

      System.out.println("about to build: " + outputJar);
      JarCreator.buildJar(jarCreatorArgs);
      System.out.println("built: " + outputJar);

      deletePath(tmpPath);

      frege.runtime.Runtime.stdout.get().close();
      frege.runtime.Runtime.stderr.get().close();

    } finally { frege.run.Concurrent.shutDownIfExists(); }
  }
}
