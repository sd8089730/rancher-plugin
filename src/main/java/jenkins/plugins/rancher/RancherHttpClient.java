package jenkins.plugins.rancher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class RancherHttpClient {

    private final String accessKey;
    private final String secretKey;
    private final String endpoint;

    public RancherHttpClient(String endpoint, String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
    }

    protected <T> T get(String url, Class<T> responseClass) throws IOException {
        HttpGet getMethod = new HttpGet(endpoint + url);
        return execute(getMethod, responseClass);
    }

    protected <T> T delete(String url, Class<T> responseClass) throws IOException {
        HttpDelete deleteMethod = new HttpDelete(endpoint + url);
        return execute(deleteMethod, responseClass);
    }

    protected <T> T post(String url, Object data, Class<T> responseClass) throws IOException {
        HttpPost postMethod = new HttpPost(endpoint + url);
        postMethod.setEntity(getRequestBody(data));
        return execute(postMethod, responseClass);
    }

    protected <T> T put(String url, Object data, Class<T> responseClass) throws IOException {
        HttpPut putMethod = new HttpPut(endpoint + url);
        putMethod.setEntity(getRequestBody(data));
        return execute(putMethod, responseClass);
    }

    private <T> T execute(HttpUriRequest request, Class<T> responseClass) throws IOException {
        request.setHeader("Authorization", getAuthorization());
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());

        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_ACCEPTED && statusCode != HttpStatus.SC_CREATED) {
            throw new RuntimeException(String.format("Some Error Happen statusCode %d response: %s", statusCode, responseBody));
        }

        return getObjectMapper().readValue(responseBody, responseClass);
    }

    private StringEntity getRequestBody(Object data) throws JsonProcessingException {
        String requestBody = getObjectMapper().writeValueAsString(data);
        return new StringEntity(requestBody, StandardCharsets.UTF_8);
    }

    private String getAuthorization() {
        byte[] encodedAuth = Base64.encodeBase64((accessKey + ":" + secretKey).getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(encodedAuth, StandardCharsets.US_ASCII);
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return objectMapper;
    }
}
