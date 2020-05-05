// Copyright 2020 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.healthcare.etl.client;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.healthcare.v1beta1.CloudHealthcare;
import com.google.api.services.healthcare.v1beta1.model.Message;
import com.google.cloud.healthcare.etl.model.streaming.Hl7v2Message;
import com.google.cloud.healthcare.etl.util.HttpUtils;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client which talks to the Cloud Healthcare API through HTTP requests. This client is created
 * mainly to encapsulate the unserializable dependencies, since most generated classes are not
 * serializable in the HTTP client.
 */
public class HttpApiClient implements ApiClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpApiClient.class);

  private static final Set<String> EMPTY_BUNDLES = Sets.newHashSet(
      "", "{}",
      "{\"resourceType\":\"Bundle\",\"type\":\"transaction\"}",
      "{\"type\":\"transaction\",\"resourceType\":\"Bundle\"}"
  );

  private static final String HEADER_VAL_CONTENT_TYPE = "application/fhir+json";
  private static final String HEADER_VAL_ACCEPT = "application/fhir+json; charset=utf-8";
  private static final String HEADER_KEY_ACCEPT_CHARSET = "Accept-Charset";
  private static final String HEADER_VAL_ACCEPT_CHARSET = "utf-8";

  private transient CloudHealthcare client;
  private transient GoogleCredential credential;

  public HttpApiClient() throws IOException {
    initClient();
  }

  public Hl7v2Message getMessage(String msgName) throws IOException, ParseException {
    Message msg = client.projects().locations().datasets().hl7V2Stores().messages().get(msgName)
        .execute();
    return Hl7v2Message.fromModel(msg);
  }

  // TODO (b/129274209): Revert back to using Healthcare client once it is fixed.
  public String commitBundle(String fhirStoreName, String bundle)
      throws URISyntaxException, IOException {
    if (EMPTY_BUNDLES.contains(bundle)) {
      LOGGER.info(String.format("Bundle %s is empty, skip committing.", bundle));
      return "";
    }

    HttpClient httpClient = HttpClients.createDefault();
    URIBuilder uriBuilder = new URIBuilder(
        client.getRootUrl() + "v1beta1/" + fhirStoreName + "/fhir")
        .setParameter("access_token", credential.getAccessToken());
    StringEntity requestEntity = new StringEntity(bundle);

    HttpUriRequest request = RequestBuilder
        .post()
        .setUri(uriBuilder.build())
        .setEntity(requestEntity)
        .addHeader("Content-Type", HEADER_VAL_CONTENT_TYPE)
        .addHeader(HEADER_KEY_ACCEPT_CHARSET, HEADER_VAL_ACCEPT_CHARSET)
        .addHeader("Accept", HEADER_VAL_ACCEPT)
        .build();

    HttpResponse response = httpClient.execute(request);
    HttpEntity responseEntity = response.getEntity();
    String content = EntityUtils.toString(responseEntity);
    if (!HttpUtils.isSuccess(response.getStatusLine().getStatusCode())) {
      throw new RuntimeException(content);
    }
    return content;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    initClient();
  }

  private void initClient() throws IOException {
    HttpTransport transport = new NetHttpTransport();
    GsonFactory jsonFactory = new GsonFactory();
    credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);
    client = new CloudHealthcare(transport, jsonFactory, credential);
  }
}
