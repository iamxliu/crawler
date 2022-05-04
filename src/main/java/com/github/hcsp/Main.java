package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("http://sina.cn");
            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
//                System.out.println(response1.getCode() + " " + response1.getReasonPhrase());
                System.out.println(response1.getStatusLine());
                HttpEntity entity1 = response1.getEntity();
                EntityUtils.consume(entity1);
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
