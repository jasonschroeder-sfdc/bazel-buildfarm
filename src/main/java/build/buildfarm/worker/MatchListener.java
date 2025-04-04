// Copyright 2021 The Buildfarm Authors. All rights reserved.
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

import build.buildfarm.common.Claim;
import build.buildfarm.v1test.QueueEntry;
import javax.annotation.Nullable;

public interface MatchListener {
  // start/end pair called for each wait period
  // if this returns false, the wait does not begin, and onEntry will be called with 'null'
  boolean onWaitStart();

  void onWaitEnd();

  // returns false if this listener will not handle this match
  boolean onEntry(@Nullable QueueEntry queueEntry, Claim resource) throws InterruptedException;

  void onError(Throwable t);
}
