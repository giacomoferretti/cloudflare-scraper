# Cloudflare Scraper <img align="right" src="https://i.imgur.com/9g5TRw1.png" height="200" width="200">

[![Build Status](https://travis-ci.com/giacomoferretti/CloudflareScraper.svg?branch=master)](https://travis-ci.com/giacomoferretti/CloudflareScraper)
[![Latest](https://api.bintray.com/packages/hexile/maven/CloudflareScraper/images/download.svg)](https://bintray.com/hexile/maven/CloudflareScraper/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/giacomoferretti/CloudflareScraper/blob/master/LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b0e83b55f5d24dcd8fe963a0ba664aec)](https://www.codacy.com/app/giacomoferretti/CloudflareScraper?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=giacomoferretti/CloudflareScraper&amp;utm_campaign=Badge_Grade)
#### [View Releases and Changelogs](https://github.com/giacomoferretti/CloudflareScraper/releases)

This is a libray written in Java that helps you solve the IUAM challenge in websites protected by Cloudflare.

## Example code
```java
public class Main {

    public static void main(String... args) throws URISyntaxException, IOException, ScraperException, InterruptedException, ScriptException {
        // Create CloudflareScraper object
        CloudflareScraper cloudflareScraper = new CloudflareScraperBuilder(new URI("URL"))
                .setConnectionTimeout(5000)
                .setReadTimeout(5000)
                .setChallengeDelay(4000) // At least 4000 milliseconds, otherwise Cloudflare won't give you a clearance cookie
                .build();

        // Check if site is protected by Cloudflare
        if (cloudflareScraper.connect()) {
            // Get necessary cookies
            List<HttpCookie> cookies = cloudflareScraper.getCookies();
        }
    }
}
```

## Usage

#### 1. Create the CloudflareScraper Object

You can create a [CloudflareScraper](https://github.com/giacomoferretti/CloudflareScraper/blob/master/src/main/java/xyz/hexile/cloudflarescraper/CloudflareScraper.java) Object via its own constructor or by using the [CloudflareScraperBuilder](https://github.com/giacomoferretti/CloudflareScraper/blob/master/src/main/java/xyz/hexile/cloudflarescraper/CloudflareScraperBuilder.java) Object.

**Using constructor**:

```java
CloudflareScraper cloudflareScraper = new CloudflareScraper(new URI("URL"));
```

**Using CloudflareScraperBuilder**:

```java
CloudflareScraper cloudflareScraper = new CloudflareScraper.Builder(new URI("URL"))
                .setChallengeDelay(4000) // At least 4000 milliseconds
                .setConnectionTimeout(5000)
                .setReadTimeout(5000)
                .build();
```

## Download

**Maven**
```xml
<dependency>
    <groupId>xyz.hexile</groupId>
    <artifactId>cloudflarescraper</artifactId>
    <version>1.0-alpha1</version>
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
    compile 'xyz.hexile:cloudflarescraper:1.0-alpha1'
}

repositories {
    jcenter()
}
```
