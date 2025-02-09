/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.metrics.micrometer.reporter;

import org.apache.iotdb.db.metrics.micrometer.registry.IoTDBMeterRegistry;
import org.apache.iotdb.db.metrics.micrometer.registry.IoTDBRegistryConfig;
import org.apache.iotdb.metrics.MetricManager;
import org.apache.iotdb.metrics.Reporter;
import org.apache.iotdb.metrics.utils.ReporterType;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class MicrometerIoTDBReporter implements Reporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerIoTDBReporter.class);
  private MetricManager metricManager;

  @Override
  public boolean start() {
    try {
      Set<MeterRegistry> meterRegistrySet =
          Metrics.globalRegistry.getRegistries().stream()
              .filter(reporter -> reporter instanceof IoTDBMeterRegistry)
              .collect(Collectors.toSet());
      IoTDBMeterRegistry ioTDBMeterRegistry;
      if (meterRegistrySet.size() == 0) {
        ioTDBMeterRegistry = new IoTDBMeterRegistry(IoTDBRegistryConfig.DEFAULT, Clock.SYSTEM);
        Metrics.addRegistry(ioTDBMeterRegistry);
      } else {
        ioTDBMeterRegistry = (IoTDBMeterRegistry) meterRegistrySet.toArray()[0];
      }
      ioTDBMeterRegistry.start(new NamedThreadFactory("iotdb-metrics-publisher"));
    } catch (Exception e) {
      LOGGER.error("Failed to start Micrometer IoTDBReporter, because {}", e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public boolean stop() {
    try {
      Set<MeterRegistry> meterRegistrySet =
          Metrics.globalRegistry.getRegistries().stream()
              .filter(reporter -> reporter instanceof IoTDBMeterRegistry)
              .collect(Collectors.toSet());
      for (MeterRegistry meterRegistry : meterRegistrySet) {
        if (!meterRegistry.isClosed()) {
          meterRegistry.close();
          Metrics.removeRegistry(meterRegistry);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Failed to stop Micrometer IoTDBReporter, because {}", e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public ReporterType getReporterType() {
    return ReporterType.iotdb;
  }

  @Override
  public void setMetricManager(MetricManager metricManager) {
    this.metricManager = metricManager;
  }
}
