// Copyright 2017 The Buildfarm Authors. All rights reserved.
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

package build.buildfarm.worker;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.transform;
import static com.google.common.util.concurrent.Futures.transformAsync;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.Collections.synchronizedList;

import build.bazel.remote.execution.v2.Action;
import build.bazel.remote.execution.v2.Command;
import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.Directory;
import build.bazel.remote.execution.v2.DirectoryNode;
import build.buildfarm.cas.cfc.CASFileCache;
import build.buildfarm.common.DigestUtil;
import build.buildfarm.common.io.Directories;
import build.buildfarm.worker.ExecDirException.ViolationException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.extern.java.Log;

@Log
public class CFCLinkExecFileSystem extends CFCExecFileSystem {
  // perform first-available non-output symlinking and retain directories in cache
  private final boolean linkInputDirectories;

  // indicate symlinking above for a set of matching paths
  private final Iterable<Pattern> linkedInputDirectories;

  private final Map<Path, Iterable<String>> rootInputFiles = new ConcurrentHashMap<>();
  private final Map<Path, Iterable<Digest>> rootInputDirectories = new ConcurrentHashMap<>();

  public CFCLinkExecFileSystem(
      Path root,
      CASFileCache fileCache,
      @Nullable UserPrincipal owner,
      boolean linkInputDirectories,
      Iterable<String> linkedInputDirectories,
      boolean allowSymlinkTargetAbsolute,
      ExecutorService removeDirectoryService,
      ExecutorService accessRecorder,
      ExecutorService fetchService) {
    super(
        root,
        fileCache,
        owner,
        allowSymlinkTargetAbsolute,
        removeDirectoryService,
        accessRecorder,
        fetchService);
    this.linkInputDirectories = linkInputDirectories;
    this.linkedInputDirectories = Iterables.transform(linkedInputDirectories, Pattern::compile);
  }

  @SuppressWarnings("ConstantConditions")
  private ListenableFuture<Void> put(
      Digest digest, Path path, boolean isExecutable, Consumer<String> onKey) {
    if (digest.getSizeBytes() == 0) {
      return listeningDecorator(fetchService)
          .submit(
              () -> {
                Files.createFile(path);
                // ignore executable
                return null;
              });
    }
    String key = fileCache.getKey(digest, isExecutable);
    return transformAsync(
        fileCache.put(digest, isExecutable, fetchService),
        (fileCachePath) -> {
          checkNotNull(key);
          // we saw null entries in the built immutable list without synchronization
          onKey.accept(key);
          if (digest.getSizeBytes() != 0) {
            try {
              Files.createLink(path, fileCachePath);
            } catch (IOException e) {
              return immediateFailedFuture(e);
            }
          }
          return immediateFuture(null);
        },
        fetchService);
  }

  private ListenableFuture<Void> catchingPut(
      Digest digest, Path root, Path path, boolean isExecutable, Consumer<String> onKey) {
    return catching(
        put(digest, path, isExecutable, onKey),
        e -> new ViolationException(digest, root.relativize(path), isExecutable, e));
  }

  @SuppressWarnings("ConstantConditions")
  private ListenableFuture<Void> linkDirectory(
      Path execPath, Digest digest, Map<Digest, Directory> directoriesIndex) {
    return transformAsync(
        fileCache.putDirectory(digest, directoriesIndex, fetchService),
        pathResult -> {
          Path path = pathResult.getPath();
          if (pathResult.isMissed()) {
            log.finer(
                String.format(
                    "putDirectory(%s, %s) created", execPath, DigestUtil.toString(digest)));
          }
          Files.createSymbolicLink(execPath, path);
          return immediateFuture(null);
        },
        fetchService);
  }

  private static void checkExecErrors(Path path, List<Throwable> errors) throws ExecDirException {
    if (!errors.isEmpty()) {
      throw new ExecDirException(path, errors);
    }
  }

  private static Iterator<String> directoriesIterator(
      Digest digest, Map<Digest, Directory> directoriesIndex) {
    Directory root = directoriesIndex.get(digest);
    return new Iterator<>() {
      boolean atEnd = root.getDirectoriesCount() == 0;
      Stack<String> path = new Stack<>();
      Stack<Iterator<DirectoryNode>> route = new Stack<>();
      Iterator<DirectoryNode> current = root.getDirectoriesList().iterator();

      @Override
      public boolean hasNext() {
        return !atEnd;
      }

      @Override
      public String next() {
        String nextPath;
        DirectoryNode next = current.next();
        String name = next.getName();
        path.push(name);
        nextPath = String.join("/", path);
        Digest digest = next.getDigest();
        if (digest.getSizeBytes() != 0) {
          route.push(current);
          current = directoriesIndex.get(digest).getDirectoriesList().iterator();
        } else {
          path.pop();
        }
        while (!current.hasNext() && !route.isEmpty()) {
          current = route.pop();
          path.pop();
        }
        atEnd = !current.hasNext();
        return nextPath;
      }
    };
  }

  private Set<String> linkedDirectories(
      Map<Digest, Directory> directoriesIndex, Digest rootDigest) {
    // skip this search if all the directories are real
    if (linkInputDirectories) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();

      Iterator<String> dirs = directoriesIterator(rootDigest, directoriesIndex);
      while (dirs.hasNext()) {
        String dir = dirs.next();
        for (Pattern pattern : linkedInputDirectories) {
          if (pattern.matcher(dir).matches()) {
            builder.add(dir);
            break; // avoid adding the same directory twice
          }
        }
      }
      return builder.build();
    }
    return ImmutableSet.of();
  }

  @VisibleForTesting
  static OutputDirectory createOutputDirectory(Command command) {
    Iterable<String> files;
    Iterable<String> dirs;
    if (command.getOutputPathsCount() != 0) {
      files = command.getOutputPathsList();
      dirs = ImmutableList.of(); // output paths require the action to create their own directory
    } else {
      files = command.getOutputFilesList();
      dirs = command.getOutputDirectoriesList();
    }
    if (!command.getWorkingDirectory().isEmpty()) {
      files = Iterables.transform(files, file -> command.getWorkingDirectory() + "/" + file);
      dirs = Iterables.transform(dirs, dir -> command.getWorkingDirectory() + "/" + dir);
    }
    return OutputDirectory.parse(files, dirs, command.getEnvironmentVariablesList());
  }

  class LinkExecFileVisitor extends ExecFileVisitor {
    private final Set<Path> linkedDirectories; // only need contains
    private final Map<Digest, Directory> index; // only need retrieve
    private final OutputDirectory outputDirectoryRoot;
    private final Stack<OutputDirectory> outputDirectories = new Stack<>();
    private final List<String> inputFiles = synchronizedList(new ArrayList<>());
    private final List<Digest> inputDirectories = synchronizedList(new ArrayList<>());

    LinkExecFileVisitor(
        Set<Path> linkedDirectories,
        Map<Digest, Directory> index,
        OutputDirectory outputDirectoryRoot) {
      this.linkedDirectories = linkedDirectories;
      this.index = index;
      this.outputDirectoryRoot = outputDirectoryRoot;
    }

    List<String> inputFiles() {
      return inputFiles;
    }

    List<Digest> inputDirectories() {
      return inputDirectories;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      OutputDirectory outputDirectory;
      if (outputDirectories.isEmpty()) {
        outputDirectory = outputDirectoryRoot;
      } else {
        String name = dir.getFileName().toString();
        OutputDirectory parentOutputDirectory = outputDirectories.peek();
        outputDirectory =
            parentOutputDirectory != null ? parentOutputDirectory.getChild(name) : null;
      }
      if (outputDirectory == null && linkedDirectories.contains(dir)) {
        Digest digest = (Digest) attrs.fileKey();
        futures.add(
            transform(
                linkDirectory(dir, digest, index),
                result -> {
                  inputDirectories.add(digest);
                  return result;
                },
                fetchService));
        return FileVisitResult.SKIP_SUBTREE;
      }

      FileVisitResult result = super.preVisitDirectory(dir, attrs);
      if (result == FileVisitResult.CONTINUE) {
        outputDirectories.push(outputDirectory);
      }
      return result;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      // this is only called when we've continued and placed onto stack
      outputDirectories.pop();
      return super.postVisitDirectory(dir, exc);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      ListenableFuture<Void> populate;
      boolean terminate = false;
      if (attrs.isSymbolicLink()) {
        ExecSymlinkAttributes symlinkAttrs = (ExecSymlinkAttributes) attrs;
        populate = putSymlink(file, symlinkAttrs.target());
      } else if (attrs.isRegularFile()) {
        Digest digest = (Digest) attrs.fileKey();
        ExecFileAttributes fileAttrs = (ExecFileAttributes) attrs;
        // mild risk here with inputFiles missing a key that was referenced...
        populate = catchingPut(digest, root, file, fileAttrs.isExecutable(), inputFiles::add);
      } else {
        populate = immediateFailedFuture(new IOException("unknown file type for " + file));
        terminate = true;
      }
      futures.add(populate);
      return terminate ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
    }
  }

  @Override
  public Path createExecDir(
      String operationName, Map<Digest, Directory> directoriesIndex, Action action, Command command)
      throws IOException, InterruptedException {
    Digest inputRootDigest = action.getInputRootDigest();
    OutputDirectory outputDirectory = createOutputDirectory(command);

    Path execDir = root().resolve(operationName);
    if (Files.exists(execDir)) {
      Directories.remove(execDir, fileStore);
    }
    Files.createDirectories(execDir);

    Set<Path> linkedInputDirectories =
        linkInputDirectories
            ? ImmutableSet.copyOf(
                Iterables.transform(
                    linkedDirectories(directoriesIndex, inputRootDigest), execDir::resolve))
            : ImmutableSet.of(); // does this work on windows with / separators?

    log.log(Level.FINER, operationName + " walking execTree");
    ExecTree execTree = new ExecTree(directoriesIndex);
    LinkExecFileVisitor visitor =
        new LinkExecFileVisitor(linkedInputDirectories, directoriesIndex, outputDirectory);
    execTree.walk(execDir, action.getInputRootDigest(), visitor);
    Iterable<ListenableFuture<Void>> fetchedFutures = visitor.futures();
    boolean success = false;
    try {
      InterruptedException exception = null;
      boolean wasInterrupted = false;
      ImmutableList.Builder<Throwable> exceptions = ImmutableList.builder();
      for (ListenableFuture<Void> fetchedFuture : fetchedFutures) {
        if (exception != null || wasInterrupted) {
          fetchedFuture.cancel(true);
        } else {
          try {
            fetchedFuture.get();
          } catch (ExecutionException e) {
            // just to ensure that no other code can react to interrupt status
            exceptions.add(e.getCause());
          } catch (InterruptedException e) {
            fetchedFuture.cancel(true);
            exception = e;
          }
        }
        wasInterrupted = Thread.interrupted() || wasInterrupted;
      }
      if (wasInterrupted) {
        Thread.currentThread().interrupt();
        // unlikely, but worth guarding
        if (exception == null) {
          exception = new InterruptedException();
        }
      }
      if (exception != null) {
        throw exception;
      }
      checkExecErrors(execDir, exceptions.build());
      success = true;
    } finally {
      if (!success) {
        fileCache.decrementReferences(visitor.inputFiles(), visitor.inputDirectories());
        Directories.remove(execDir, fileStore);
      }
    }

    rootInputFiles.put(execDir, visitor.inputFiles());
    rootInputDirectories.put(execDir, visitor.inputDirectories());

    log.log(Level.FINER, operationName + " stamping output directories");
    boolean stamped = false;
    try {
      outputDirectory.stamp(execDir);
      stamped = true;
    } finally {
      if (!stamped) {
        destroyExecDir(execDir);
      }
    }
    if (owner != null) {
      Directories.setAllOwner(execDir, owner);
    }
    return execDir;
  }

  @Override
  public void destroyExecDir(Path execDir) throws IOException, InterruptedException {
    Iterable<String> inputFiles = rootInputFiles.remove(execDir);
    Iterable<Digest> inputDirectories = rootInputDirectories.remove(execDir);
    if (inputFiles != null || inputDirectories != null) {
      fileCache.decrementReferences(
          inputFiles == null ? ImmutableList.of() : inputFiles,
          inputDirectories == null ? ImmutableList.of() : inputDirectories);
    }
    super.destroyExecDir(execDir);
  }
}
