// Copyright 2025 The Buildfarm Authors. All rights reserved.
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

package build.buildfarm.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ThreadFactory;

/**
 * Utility class for creating ThreadFactory instances with consistent naming conventions.
 */
public class ThreadFactoryUtils {
  /**
   * Creates a ThreadFactory for single or multiple threads with a name format.
   *
   * @param nameFormat the format string for thread names (e.g., "MyThread-%d" for numbered
   *     threads)
   * @return a ThreadFactory that creates threads with the specified name format
   */
  public static ThreadFactory createNamedThreadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  /**
   * Creates a ThreadFactory for a single thread with a fixed name.
   *
   * @param name the name for the thread (will be used as-is, no numbering)
   * @return a ThreadFactory that creates threads with the specified name
   */
  public static ThreadFactory createNamedSingleThreadFactory(String name) {
    // For single threads, we want the exact name without numbering
    // ThreadFactoryBuilder will use the format as-is if no %d is present
    return new ThreadFactoryBuilder().setNameFormat(name).build();
  }
}

