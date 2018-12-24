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
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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

    private HttpCookie cfduid;

    private String challengeContent;
    private String challengeVc;
    private String challengePass;
    private String challengeAnswer;

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

    public CloudflareScraper(CloudflareScraperBuilder builder) {
        this.uri = builder.getUri();
        this.challengeDelay = builder.getChallengeDelay();
        this.connectionTimeout = builder.getConnectionTimeout();
        this.readTimeout = builder.getReadTimeout();
    }

    public boolean connect() throws IOException, ScraperException {
        HttpURLConnection connection = buildRequest(uri);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE && connection.getHeaderField("Server").startsWith("cloudflare")) {
            challengeContent = getContent(connection.getErrorStream(), connection.getContentEncoding());
            if (challengeContent.contains("jschl_vc") && challengeContent.contains("jschl_answer")) {
                challengeVc = extractElement(challengeContent, "input[type=hidden][name=jschl_vc]");
                challengePass = extractElement(challengeContent, "input[type=hidden][name=pass]");
                cfduid = getCookieHeader(connection.getHeaderFields(), "__cfduid");
                connection.disconnect();
                return true;
            } else {
                connection.disconnect();
                throw new ScraperException("Cannot find Cloudflare Challenge. Maybe they changed the method.");
            }
        } else {
            connection.disconnect();
            return false;
        }
    }


    public List<HttpCookie> getCookies() throws IOException, URISyntaxException, InterruptedException, ScraperException, ScriptException {
        challengeAnswer = getAnswer(extractChallenge(challengeContent));

        HttpURLConnection connection = buildRequest(generateAnswerURI(uri, challengeVc, challengePass, challengeAnswer));
        connection.setRequestProperty("Cookie", cfduid.toString());

        Thread.sleep(challengeDelay);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
            List<HttpCookie> cookies = new ArrayList<>();
            cookies.add(cfduid);
            cookies.add(getCookieHeader(connection.getHeaderFields(), "cf_clearance"));
            return cookies;
        }

        throw new ScraperException("There was an error.");
    }

    private String extractChallenge(String content) throws ScraperException {
        String urlLength = String.valueOf(getDomainName(uri).length());

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
        challenge = challenge.replaceAll("\\s{3,}[a-z](?: = |\\.).+", "").replace("t.length", urlLength);
        challenge = challenge.replaceAll("[\\n\\\\']", "");

        if (!challenge.contains("toFixed")) throw new ScraperException("Error parsing Cloudflare IUAM Javascript challenge. Please report to @author@ [@github@].");

        pattern = Pattern.compile("var s,t,o,p,b,r,e,a,k,i,n,g,f, (.*?)\":");
        matcher = pattern.matcher(challenge);
        String challengeVarName = "";
        if (matcher.find()) {
            challengeVarName = matcher.group(1).replace("={\"", ".");
        }

        challenge = challenge.replace("+" + challengeVarName + ".toFixed(10) + " + urlLength, challengeVarName + " = (" + challengeVarName + " + " + urlLength + ").toFixed(10)");
        challenge += ";\nvar answer = " + challengeVarName + ";";

        return challenge;
    }

    private static String getAnswer(String challenge) throws ScriptException {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.eval(challenge);
        return engine.get("answer").toString();
    }

    private static String extractElement(String content, String cssQuery) throws ScraperException {
        Document document = Jsoup.parse(content);
        Element element = document.selectFirst(cssQuery);
        if (element == null) throw new ScraperException("Cannot find element { " + cssQuery + " }.");
        return element.attr("value");
    }

    private static URI generateAnswerURI(URI uri, String challengeVc, String challengePass, String challengeAnswer) throws UnsupportedEncodingException, URISyntaxException {
        return new URI(uri.toString()
                + "cdn-cgi/l/chk_jschl?jschl_vc=" + URLEncoder.encode(challengeVc, StandardCharsets.UTF_8.toString())
                + "&pass=" + URLEncoder.encode(challengePass, StandardCharsets.UTF_8.toString())
                + "&jschl_answer=" + challengeAnswer);
    }

    private HttpURLConnection buildRequest(URI url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        return connection;
    }

    private static HttpCookie getCookieHeader(Map<String, List<String>> headers, String query) {
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (header.getKey() != null && header.getKey().equalsIgnoreCase("Set-Cookie") && HttpCookie.parse(header.getValue().get(0)).get(0).getName().equals(query)) {
                return HttpCookie.parse(header.getValue().get(0)).get(0);
            }
        }
        return null;
    }

    /**
     * Method that converts an <code>InputStream</code> into a <code>String</code> using <code>IOUtils</code>.
     * @param inputStream InputStream
     * @return Returns String built from InputStream.
     * @throws IOException Needed throw.
     */
    private static String getContent(InputStream inputStream) throws IOException {
        return new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
    }

    /**
     *
     * @param inputStream
     * @param contentEncoding
     * @return <code>String</code> from <code>InputStream</code> with content encoding.
     * @throws IOException if something goes wrong.
     */
    private static String getContent(InputStream inputStream, String contentEncoding) throws IOException {
        if (Objects.equals(contentEncoding, "gzip"))
            return new String(IOUtils.toByteArray(new GZIPInputStream(inputStream)), StandardCharsets.UTF_8);
        else
            return getContent(inputStream);
    }

    /**
     *
     * @param uri Target's URI object.
     * @return Stripped String that's needed for the challenge.
     */
    private static String getDomainName(URI uri) {
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }
}
