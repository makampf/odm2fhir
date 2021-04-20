package de.difuture.uds.odm2fhir.util;

/*
 * Copyright (C) 2021 DIFUTURE (https://difuture.de)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, copies
 * of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

public class HTTPHelper {

  private HTTPHelper() {}

  public static CloseableHttpClient HTTP_CLIENT;

  static {
    try {
      var connectionManager = new PoolingHttpClientConnectionManager();
      connectionManager.setMaxTotal(100);
      connectionManager.setDefaultMaxPerRoute(100);

      HTTP_CLIENT = HttpClientBuilder.create()
          .setConnectionManager(connectionManager)
          .setSSLContext(SSLContextBuilder.create().loadTrustMaterial(null, (certificate, authType) -> true).build())
          .setSSLHostnameVerifier(new NoopHostnameVerifier())
          .build();
    } catch (Exception exception) {
      // Nothing to be done here as an exception will never be thrown here...
    }
  }

  public static IClientInterceptor createAuthInterceptor(String basicauthUsername, String basicauthPassword,
                                                         String oauth2TokenURL, String oauth2ClientId, String oauth2ClientSecret)
                                                        throws IOException {
    IClientInterceptor clientInterceptor = new BasicAuthInterceptor(basicauthUsername, basicauthPassword);

    if (isNoneBlank(oauth2TokenURL, oauth2ClientId, oauth2ClientSecret)) {
      var httpPost = RequestBuilder
          .post(oauth2TokenURL)
          .addParameter("grant_type", "client_credentials")
          .addParameter("client_id", oauth2ClientId)
          .addParameter("client_secret", oauth2ClientSecret)
          .build();

      var content = HTTP_CLIENT.execute(httpPost).getEntity().getContent();

      var token = new ObjectMapper().readTree(content).get("access_token").textValue();

      clientInterceptor = new BearerTokenAuthInterceptor(token);
    }

    return clientInterceptor;
  }

}