package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private int requestCount = 0;
    private long startTime = 0;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void createDocument(Object document, String signature) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();

        try {
            JsonNode documentNode = objectMapper.valueToTree(document);
            root.set("document", documentNode);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка при сериализации документа в JSON формат");
            throw new RuntimeException(e);
        }

        root.put("signature", signature);
        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            System.err.println("Ошибка при сериализации документа в JSON формат");
            throw new RuntimeException(e);
        }
        if (requestCount == 0) {
            startTime = System.currentTimeMillis();
        }

        if (requestCount >= requestLimit) {
            long remainingTime = timeUnit.toMillis(1) - (System.currentTimeMillis() - startTime);
            try {
                requestCount = 0;
                TimeUnit.MILLISECONDS.sleep(remainingTime);
                startTime = System.currentTimeMillis();
            } catch (InterruptedException e) {
                System.err.println("ошибка при ожидании");
            }
        }

        HttpPost httpPostRequest = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
        httpPostRequest.addHeader("Content-type", "application/json");
        httpPostRequest.setEntity(new StringEntity(requestBody, "UTF-8"));
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpResponse response = httpClient.execute(httpPostRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("HTTP статус, код ответа: " + statusCode);
        } catch (IOException e) {
            System.err.println("Ошибка при отправке POST запроса и получения ответа");
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии HttpClient");
            }

            requestCount++;
        }

    }
}