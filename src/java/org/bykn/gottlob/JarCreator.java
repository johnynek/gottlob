// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.bykn.gottlob;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * A class for creating Jar files. Allows normalization of Jar entries by setting their timestamp to
 * the DOS epoch. All Jar entries are sorted alphabetically.
 */
public class JarCreator extends JarHelper {

  // Map from Jar entry names to files. Use TreeMap so we can establish a canonical order for the
  // entries regardless in what order they get added.
  private final Map<String, String> jarEntries = new TreeMap<>();
  private String manifestFile;
  private String mainClass;

  public JarCreator(String fileName) {
    super(fileName);
  }

  /**
   * Adds an entry to the Jar file, normalizing the name.
   *
   * @param entryName the name of the entry in the Jar file
   * @param fileName the name of the input file for the entry
   * @return true iff a new entry was added
   */
  public boolean addEntry(String entryName, String fileName) {
    if (entryName.startsWith("/")) {
      entryName = entryName.substring(1);
    } else if (entryName.startsWith("./")) {
      entryName = entryName.substring(2);
    }
    return jarEntries.put(entryName, fileName) == null;
  }

  /**
   * Adds the contents of a directory to the Jar file. All files below this
   * directory will be added to the Jar file using the name relative to the
   * directory as the name for the Jar entry.
   *
   * @param directory the directory to add to the jar
   */
  public void addDirectory(File directory) {
    addDirectory(null, directory);
  }

  public void addJar(File file) {
    jarEntries.put(file.getAbsolutePath(), file.getAbsolutePath());
  }
  /**
   * Adds the contents of a directory to the Jar file. All files below this
   * directory will be added to the Jar file using the prefix and the name
   * relative to the directory as the name for the Jar entry. Always uses '/' as
   * the separator char for the Jar entries.
   *
   * @param prefix the prefix to prepend to every Jar entry name found below the
   *        directory
   * @param directory the directory to add to the Jar
   */
  private void addDirectory(String prefix, File directory) {
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        String entryName = prefix != null ? prefix + "/" + file.getName() : file.getName();
        jarEntries.put(entryName, file.getAbsolutePath());
        if (file.isDirectory()) {
          addDirectory(entryName, file);
        }
      }
    }
  }

  /**
   * Adds a collection of entries to the jar, each with a given source path, and with
   * the resulting file in the root of the jar.
   * <pre>
   * some/long/path.foo => (path.foo, some/long/path.foo)
   * </pre>
   */
  public void addRootEntries(Collection<String> entries) {
    for (String entry : entries) {
      jarEntries.put(new File(entry).getName(), entry);
    }
  }

  /**
   * Sets the main.class entry for the manifest. A value of <code>null</code>
   * (the default) will omit the entry.
   *
   * @param mainClass the fully qualified name of the main class
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  /**
   * Sets filename for the manifest content. If this is set the manifest will be
   * read from this file otherwise the manifest content will get generated on
   * the fly.
   *
   * @param manifestFile the filename of the manifest file.
   */
  public void setManifestFile(String manifestFile) {
    this.manifestFile = manifestFile;
  }

  private byte[] manifestContent() throws IOException {
    Manifest manifest;
    if (manifestFile != null) {
      FileInputStream in = new FileInputStream(manifestFile);
      manifest = new Manifest(in);
    } else {
      manifest = new Manifest();
    }
    Attributes attributes = manifest.getMainAttributes();
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    Attributes.Name createdBy = new Attributes.Name("Created-By");
    if (attributes.getValue(createdBy) == null) {
      attributes.put(createdBy, "blaze");
    }
    if (mainClass != null) {
      attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    manifest.write(out);
    return out.toByteArray();
  }

  /**
   * Executes the creation of the Jar file.
   *
   * @throws IOException if the Jar cannot be written or any of the entries
   *         cannot be read.
   */
  public void execute() throws IOException {
    out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)));

    // Create the manifest entry in the Jar file
    writeManifestEntry(manifestContent());
    try {
      for (Map.Entry<String, String> entry : jarEntries.entrySet()) {
        copyEntry(entry.getKey(), new File(entry.getValue()));
      }
    } finally {
      out.closeEntry();
      out.close();
    }
  }

  @Override
  protected boolean ignoreFileName(String name) {
    /*
     * It does not make sense to copy signature files
     * into a fat jar because the jar signature will
     * be broken
aaaaaaaaaaaaaœ:00
     */
    return (name.endsWith(".DSA") || name.endsWith(".RSA"));
  }

  public static void buildJar(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("usage: CreateJar [-m manifest] output [root directories]");
      System.exit(1);
    }

    int idx = 0;
    String manifestFile = null;
    if (args[0].equals("-m")) {
      manifestFile = args[1];
      idx = 2;
    }
    String output = args[idx];
    JarCreator createJar = new JarCreator(output);
    createJar.setManifestFile(manifestFile);
    for (int i = (idx+1); i < args.length; i++) {
      String thisName = args[i];
      File f = new File(thisName);
      if (JarHelper.isJar(f)) {
        createJar.addJar(f);
      }
      else {
        createJar.addDirectory(f);
      }
    }
    createJar.setCompression(true);
    createJar.setNormalize(true);
    createJar.execute();
  }

  /**
   * A simple way to create Jar file using the JarCreator class.
   */
  public static void main(String[] args) {
    try {
      buildJar(args);
   } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
