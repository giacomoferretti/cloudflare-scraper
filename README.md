<!-- Title -->
# Cloudflare Scraper <img align="right" src="https://i.imgur.com/9g5TRw1.png" height="200" width="200">

<!-- Badges -->
[![Build Status](https://travis-ci.com/giacomoferretti/CloudflareScraper.svg?branch=master)](https://travis-ci.com/giacomoferretti/CloudflareScraper)
[![Latest](https://api.bintray.com/packages/hexile/maven/CloudflareScraper/images/download.svg)](https://bintray.com/hexile/maven/CloudflareScraper/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/giacomoferretti/CloudflareScraper/blob/master/LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b0e83b55f5d24dcd8fe963a0ba664aec)](https://www.codacy.com/app/giacomoferretti/CloudflareScraper?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=giacomoferretti/CloudflareScraper&amp;utm_campaign=Badge_Grade)

<!-- #### [View Releases and Changelogs](https://github.com/giacomoferretti/CloudflareScraper/releases) [unused]-->

This is a libray written in Java that helps you solve the IUAM challenge in websites protected by Cloudflare.

<!-- Example code -->
## Example code
```java
public static void main(String... args) throws URISyntaxException, IOException, ScraperException, InterruptedException, ScriptException {
    // Create CloudflareScraper object
    CloudflareScraper cloudflareScraper = new CloudflareScraperBuilder(new URI("URL"))
            .setConnectionTimeout(5000)
            .setReadTimeout(5000)
            .setChallengeDelay(4000) // At least 4000 milliseconds, otherwise Cloudflare won't give you a clearance cookie
            .build();

    // Check if site is protected by Cloudflare
    if (cloudflareScraper.connect()) {

        // Pass this cookies in your request
        List<HttpCookie> cookies = cloudflareScraper.getCookies();
    }
}
```

**Add cookies using** `HttpURLConnection`
```java
HttpURLConnection connection = (HttpURLConnection) new URL("URL").openConnection();
for (HttpCookie cookie : cookies) {
    connection.setRequestProperty("Cookie", cookie.toString());
}
```

**Add cookies using** `OkHttpClient`
```java
OkHttpClient okHttpClient = new OkHttpClient();
Request.Builder requestBuilder = new Request.Builder().url("URL");
for (HttpCookie cookie : cookies) {
    requestBuilder.addHeader("Cookie", cookie.toString());
}

Request request = requestBuilder.build();
```

<!-- Download section -->
## Download

**Maven**
```xml
<dependency>
    <groupId>xyz.hexile</groupId>
    <artifactId>cloudflarescraper</artifactId>
    <version>1.0</version>
</dependency>
```
```xml
<repository>
    <id>jcenter</id>
    <name>jcenter-bintray</name>
    <url>http://jcenter.bintray.com</url>
</repository>
```

**Gradle**
```gradle
dependencies {
    compile 'xyz.hexile:cloudflarescraper:1.0'
}

repositories {
    jcenter()
}
```

<a href='https://bintray.com/hexile/maven/CloudflareScraper?source=watch' alt='Get automatic notifications about new "CloudflareScraper" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_bw.png'></a>