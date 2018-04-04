/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.everythingjboss.jdg.embedded_jdg_openshift;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        // Configure Infinispan to use default transport and Kubernetes configuration
        GlobalConfiguration globalConfig = new GlobalConfigurationBuilder().transport().defaultTransport()
                .addProperty("configurationFile", "default-configs/default-jgroups-kubernetes.xml").build();

        // Each node generates events, so we don't want to consume all memory available.
        // Let's limit number of entries to 100 and use sync mode
        ConfigurationBuilder cacheConfiguration = new ConfigurationBuilder();
        cacheConfiguration.clustering().cacheMode(CacheMode.DIST_SYNC);
        // cacheConfiguration.memory().evictionType(EvictionType.COUNT).size(100);

        CountDownLatch cdl = new CountDownLatch(1);

        DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfig, cacheConfiguration.build());
        Cache<String, byte[]> cache = cacheManager.getCache();
        cache.addListener(new ClusterListener());
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        if (cacheManager.isCoordinator()) {
            logger.info("*** This is the coordinator instance ***");
            cacheManager.addListener(new ClusterListener(cdl));

            // Suspend execution here till there are three nodes in the cluster
            cdl.await();

            // Necessary to wait for clustering to do its magic before we start putting
            // in entries into the cache
            Thread.sleep(1000);

            scheduler.scheduleAtFixedRate(() -> cache.put(Instant.now().toString(), new byte[4096]), 0, 1,
                    TimeUnit.SECONDS);
        }
        
        try {
            // This container will operate for an hour and then it will die
            TimeUnit.HOURS.sleep(1);
        } catch (InterruptedException e) {
            scheduler.shutdown();
            cacheManager.close();
            cacheManager.stop();
        }

    }
}
