/*
 * Copyright 2018 Giacomo Ferretti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.hexile.cloudflarescraper;

import java.net.URI;

/**
 * Used to create {@link xyz.hexile.cloudflarescraper.CloudflareScraper} instances.
 * This helps you with setting up {@link xyz.hexile.cloudflarescraper.CloudflareScraper} instances without errors.
 */
public class CloudflareScraperBuilder {
    // Required parameters
    private URI uri;

    // Optional parameters
    private int challengeDelay;
    private int connectionTimeout;
    private int readTimeout;

    public CloudflareScraperBuilder(URI uri) {
        this.uri = uri;
    }

    public CloudflareScraperBuilder setChallengeDelay(int challengeDelay) {
        this.challengeDelay = challengeDelay;
        return this;
    }

    public CloudflareScraperBuilder setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public CloudflareScraperBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public CloudflareScraper build() {
        return new CloudflareScraper(uri);
    }
}
