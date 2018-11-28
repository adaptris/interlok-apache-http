/*
 * Copyright 2018 Adaptris Ltd.
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
package com.adaptris.core.http.apache.request;

import com.adaptris.core.util.Args;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.hibernate.validator.constraints.NotBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.joining;

@XStreamAlias("apache-http-magento-hmac-signature")
public class MagentoHMACSignature extends HMACSignatureImpl {

    @NotBlank
    private String identity;

    @NotBlank
    private String oauthVersion;

    @NotBlank
    private String oauthConsumerKey;

    private String oauthVerifier;

    private Map<String, String> oauthParams;

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = Args.notBlank(identity, "identity");
    }

    public String getOauthVersion() {
        return oauthVersion;
    }

    public void setOauthVersion(String oauthVersion) {
        this.oauthVersion = Args.notBlank(oauthVersion, "oauthVersion");
    }

    public String getOauthConsumerKey() {
        return oauthConsumerKey;
    }

    public void setOauthConsumerKey(String oauthConsumerKey) {
        this.oauthConsumerKey = Args.notBlank(oauthConsumerKey, "oauthConsumerKey");
    }

    public String getOauthVerifier() {
        return oauthVerifier;
    }

    public void setOauthVerifier(String oauthVerifier) {
        this.oauthVerifier = oauthVerifier;
    }

    public String getOauthTimestamp() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public String getOauthNonce() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public <T extends MagentoHMACSignature> T withIdentity(String s) {
        setIdentity(s);
        return (T) this;
    }

    public <T extends MagentoHMACSignature> T withOauthConsumerKey(String s) {
        setOauthConsumerKey(s);
        return (T) this;
    }

    public <T extends MagentoHMACSignature> T withOauthVersion(String s) {
        setOauthVersion(s);
        return (T) this;
    }

    public <T extends MagentoHMACSignature> T withOauthVerifier(String s) {
        setOauthVerifier(s);
        return (T) this;
    }

    @Override
    protected String getStringToSign(HttpRequest request, HttpContext context) {
        buildOauthParams(request);

        String prefix = request.getRequestLine().getMethod() + AMPERSEND;
        prefix += encodeValue(context.getAttribute("http.target_host") + request.getRequestLine().getUri()) + AMPERSEND;

        String stringToSign = oauthParams.keySet().stream()
                .map(key -> keyValue(key, encodeValue(oauthParams.get(key))))
                .collect(joining(AMPERSEND, prefix, ""));

        return stringToSign;
    }

    @Override
    protected String buildHeader(String hmacSignature) {
        Args.notBlank(getIdentity(), "identity");

        String header = oauthParams.keySet().stream()
                .map(key -> keyValueWrapped(key, oauthParams.get(key)))
                .collect(joining(COMMA, getIdentity() + " ", keyValueWrapped(",oauth_signature", encodeValue(hmacSignature))));

        return header;
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String keyValueWrapped(String key, String value) {
        return keyValue(key, StringUtils.wrap(value, '"'));
    }

    private String keyValue(String key, String value) {
        return key + "=" + value;
    }

    private void buildOauthParams(HttpRequest request) {
        oauthParams = new HashMap<String, String>() {{
            put("oauth_consumer_key", getOauthConsumerKey());
            put("oauth_signature_method", getHmacAlgorithm().getName());
            put("oauth_timestamp", getOauthTimestamp());
            put("oauth_version", getOauthVersion());
            put("oauth_nonce", getOauthNonce());
            put("oauth_signature", getOauthHeaderValue(request,"oauth_signature"));
            put("oauth_token", getOauthHeaderValue(request,"oauth_token"));
            put("oauth_verifier", getOauthVerifier());
        }};

        oauthParams.values().removeIf(StringUtils::isBlank);
    }

    private String getOauthHeaderValue(HttpRequest request, String headerName) {
        Header header = request.getFirstHeader(headerName);
        return header == null ? "" : header.getValue();
    }
}
