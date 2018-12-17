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

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudflareScraper {

    private static String[] DEFAULT_USER_AGENTS = new String[]{
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/65.0.3325.181 Chrome/65.0.3325.181 Safari/537.36",
            "Mozilla/5.0 (Linux; Android 7.0; Moto G (5) Build/NPPS25.137-93-8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.137 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0_4 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11B554a Safari/9537.53",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:59.0) Gecko/20100101 Firefox/59.0",
            "Mozilla/5.0 (Windows NT 6.3; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0"
    };

    private static String USER_AGENT = DEFAULT_USER_AGENTS[new Random().nextInt(DEFAULT_USER_AGENTS.length)];

    private HttpURLConnection connection;

    private int responseCode;
    private String content;
    private String cfuid;
    private String rayId;
    private String challenge;
    private String challengeVc;
    private String challengePass;
    private String challengeAnswer;
    private String challengeVarName;

    // Parameters
    private URI uri;
    private int challengeDelay;
    private int connectionTimeout;
    private int readTimeout;

    public CloudflareScraper(URI uri) {
        this.uri = uri;
    }

    public CloudflareScraper(URI uri, int challengeDelay) {
        this.uri = uri;
        this.challengeDelay = challengeDelay;
    }

    public CloudflareScraper(URI uri, int challengeDelay, int connectionTimeout) {
        this.uri = uri;
        this.challengeDelay = challengeDelay;
        this.connectionTimeout = connectionTimeout;
    }

    public CloudflareScraper(URI uri, int challengeDelay, int connectionTimeout, int readTimeout) {
        this.uri = uri;
        this.challengeDelay = challengeDelay;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public CloudflareScraper(Builder builder) {
        System.out.println(System.getProperty("https.protocols"));
        this.uri = builder.uri;
        this.challengeDelay = builder.challengeDelay;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }

    public static class Builder {
        // Required parameters
        private URI uri;

        // Optional parameters
        private int challengeDelay;
        private int connectionTimeout;
        private int readTimeout;

        public Builder(URI uri) {
            this.uri = uri;
        }

        public Builder setChallengeDelay(int challengeDelay) {
            this.challengeDelay = challengeDelay;
            return this;
        }


        public Builder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public CloudflareScraper build() {
            return new CloudflareScraper(this);
        }
    }

    public void connect() throws IOException, ScraperException, URISyntaxException, ScriptException {
        connection = buildRequest(uri);
        responseCode = connection.getResponseCode();

        if (responseCode == 503 && connection.getHeaderField("Server").startsWith("cloudflare")) {
            content = getContent(connection.getErrorStream());
            rayId = extractRayId(content);
            challenge = extractChallenge(content);
            challengeVc = extractChallengeVc(content);
            challengePass = extractChallengePass(content);
            challengeAnswer = getAnswer();

            // Get cookies
            for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie")) {
                    cfuid = entry.getValue().get(0).split("; ")[0];
                /*for (String string : entry.getValue()) {
                    List<HttpCookie> cookies = HttpCookie.parse(string);
                    for (HttpCookie cookie : cookies) {
                        System.out.println("Cookie: " + cookie.getName() + " = " + cookie.getValue() + " || " + cookie.getDomain() + " || " + cookie.getMaxAge() + " || " + cookie.hasExpired()  + " || " + cookie.isHttpOnly() + " || " + cookie.getSecure());
                    }
                    System.out.println("Cookie: " + string);
                    for (int i = 0; i < string.split("; ").length; i++) {
                        System.out.println("\tparam: " + string.split("; ")[i]);
                        if (string.split("; ")[i].equals("expires")) {
                            //Date expiryDate = DateFormat.getDateTimeInstance(DateFormat.FULL).parse(string.split("; ")[i].split("=")[1]);
                        }
                    }
                }*/
                }
            }
        }
        connection.disconnect();
    }

    public String getAnswer() throws ScriptException {
        System.out.println(challenge);
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.eval(challenge);
        /*ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        ScriptContext context = engine.getContext();
        StringWriter writer = new StringWriter();
        context.setWriter(writer);

        engine.eval(code);

        String output = writer.toString();

        System.out.println("Script output: " + output);*/
        String test = engine.get("answer").toString();
        System.out.println(test);
        return test;
    }

    public String getCookie() throws IOException, URISyntaxException, InterruptedException {
        connection = buildRequest(new URI(uri.toString() + "cdn-cgi/l/chk_jschl?jschl_answer=" + challengeAnswer + "&jschl_vc=" + URLEncoder.encode(challengeVc, StandardCharsets.UTF_8.toString()) + "&pass=" + URLEncoder.encode(challengePass, StandardCharsets.UTF_8.toString())));
        connection.setRequestProperty("Cookie", cfuid);
        connection.setInstanceFollowRedirects(false);
        Thread.sleep(5000);
        responseCode = connection.getResponseCode();

        System.out.println(responseCode);

        String asd = "";
        // Get cookies
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Set-Cookie")) {
                asd = entry.getValue().get(0).split("; ")[0];
                /*for (String string : entry.getValue()) {
                    List<HttpCookie> cookies = HttpCookie.parse(string);
                    for (HttpCookie cookie : cookies) {
                        System.out.println("Cookie: " + cookie.getName() + " = " + cookie.getValue() + " || " + cookie.getDomain() + " || " + cookie.getMaxAge() + " || " + cookie.hasExpired()  + " || " + cookie.isHttpOnly() + " || " + cookie.getSecure());
                    }
                    System.out.println("Cookie: " + string);
                    for (int i = 0; i < string.split("; ").length; i++) {
                        System.out.println("\tparam: " + string.split("; ")[i]);
                        if (string.split("; ")[i].equals("expires")) {
                            //Date expiryDate = DateFormat.getDateTimeInstance(DateFormat.FULL).parse(string.split("; ")[i].split("=")[1]);
                        }
                    }
                }*/
            }
        }

        return asd;
    }

    public List<String> getCookies() throws InterruptedException, IOException, URISyntaxException {
        List<String> cookies = new ArrayList<>();
        cookies.add(cfuid);
        cookies.add(getCookie());
        return cookies;
    }

    public HttpURLConnection buildRequest(URI url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        return connection;
    }

    public boolean isCloudflareProtected() {
        return this.responseCode == 503
                && connection.getHeaderField("Server").startsWith("cloudflare")
                && content.contains("jschl_vc")
                && content.contains("jschl_answer");
    }

    public String getContent(InputStream inputStream) throws IOException {
        return new String(IOUtils.toByteArray(inputStream));
    }

    public String getDomainName(URI uri) {
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public String extractRayId(String content) throws ScraperException {
        Document document = Jsoup.parse(content);
        Element element = document.selectFirst("div[class=attribution]");
        if (element == null) throw new ScraperException("Cannot find \"attribution\" element. Please report to @author@ [@github@].");
        return element.text().split(": ")[1];
    }

    // TODO: Fix RegEx
    public String extractChallenge(String content) throws ScraperException {
        String url = getDomainName(uri);

        Document document = Jsoup.parse(content);
        Element element = document.selectFirst("script");
        if (element == null) throw new ScraperException("Cannot find \"script\" element. Please report to @author@ [@github@].");

        String challenge = element.outerHtml();

        Pattern pattern = Pattern.compile("setTimeout\\(function\\(\\)\\{\\s+(var s,t,o,p,b,r,e,a,k,i,n,g,f.+?\\r?\\n[\\s\\S]+?a\\.value =.+?)\\r?\\n");
        Matcher matcher = pattern.matcher(challenge);
        if (matcher.find()) challenge = matcher.group(1);
        else throw new ScraperException("Unable to identify Cloudflare IUAM Javascript on website. Please report to @author@ [@github@].");

        pattern = Pattern.compile("a\\.value = (.+ \\+ t\\.length).+");
        matcher = pattern.matcher(challenge);
        if (matcher.find()) {
            String asd = matcher.group(1);
            challenge = challenge.replaceAll("a\\.value = (.+ \\+ t\\.length).+", asd);
        }
        challenge = challenge.replaceAll("\\s{3,}[a-z](?: = |\\.).+", "").replace("t.length", Integer.toString(url.length()));
        challenge = challenge.replaceAll("[\\n\\\\']", "");

        if (!challenge.contains("toFixed")) throw new ScraperException("Error parsing Cloudflare IUAM Javascript challenge. Please report to @author@ [@github@].");

        pattern = Pattern.compile("var s,t,o,p,b,r,e,a,k,i,n,g,f, (.*?)\":");
        matcher = pattern.matcher(challenge);
        if (matcher.find()) {
            challengeVarName = matcher.group(1).replace("={\"", ".");
        }

        challenge = challenge.replace("+" + challengeVarName + ".toFixed(10) + " + url.length(), challengeVarName + " = (" + challengeVarName + " + " + url.length() + ").toFixed(10)");
        challenge += ";\nvar answer = " + challengeVarName + ";";

        return challenge;
    }

    public String extractChallengeVc(String content) throws ScraperException {
        Document document = Jsoup.parse(content);
        Element element = document.selectFirst("input[type=hidden][name=jschl_vc]");
        if (element == null) throw new ScraperException("Cannot find \"jschl_vc\" element. Please report to @author@ [@github@].");
        return element.attr("value");
    }

    public String extractChallengePass(String content) throws ScraperException {
        Document document = Jsoup.parse(content);
        Element pass = document.selectFirst("input[type=hidden][name=pass]");
        if (pass == null) throw new ScraperException("Cannot find \"pass\" element. Please report to @author@ [@github@].");
        return pass.attr("value");
    }

    public class ScraperException extends Exception {
        ScraperException(String errorMessage) {
            super(errorMessage);
        }
    }

    // Getters
    public URI getUri() {
        return uri;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getContent() {
        return content;
    }

    public String getRayId() {
        return rayId;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getChallengeVc() {
        return challengeVc;
    }

    public String getChallengePass() {
        return challengePass;
    }

    public String getChallengeAnswer() {
        return challengeAnswer;
    }
}
