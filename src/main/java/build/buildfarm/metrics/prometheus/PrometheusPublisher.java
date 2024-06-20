// Copyright 2021 The Bazel Authors. All rights reserved.
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

package build.buildfarm.metrics.prometheus;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import java.io.IOException;
import lombok.extern.java.Log;

@Log
public class PrometheusPublisher {
  private static HTTPServer server;

  public static void startHttpServer(int port) {
    if (port > 0) {
      try {
        PrometheusMeterRegistry prometheusRegistry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Metrics.globalRegistry.add(prometheusRegistry);
        server =
            HTTPServer.builder()
                .port(port)
                .registry(prometheusRegistry.getPrometheusRegistry())
                .buildAndStart();
        log.info("Started Prometheus HTTP Server on port " + port);
      } catch (IOException e) {
        log.severe("Could not start Prometheus HTTP Server on port " + port);
      }
    }
  }

  public static void stopHttpServer() {
    if (server != null) {
      server.stop();
    }
  }
}
