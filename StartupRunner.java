package com.example.demo.config;

import com.example.demo.model.WebhookResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

@Component
public class StartupRunner implements CommandLineRunner {

    private final WebClient webClient = WebClient.create();

    @Override
    public void run(String... args) {
        // Step 1: Call generateWebhook API
        WebhookResponse response = webClient.post()
                .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "John Doe",
                          "regNo": "REG12347",
                          "email": "john@example.com"
                        }
                        """)
                .retrieve()
                .bodyToMono(WebhookResponse.class)
                .block();

        if (response == null) {
            System.out.println(" Failed to get webhook response");
            return;
        }

        String webhookUrl = response.getWebhook();
        String accessToken = response.getAccessToken();

        // Step 2: Final SQL query
        String finalQuery = """
            SELECT 
                e1.EMP_ID,
                e1.FIRST_NAME,
                e1.LAST_NAME,
                d.DEPARTMENT_NAME,
                COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
            FROM EMPLOYEE e1
            JOIN DEPARTMENT d 
                ON e1.DEPARTMENT = d.DEPARTMENT_ID
            LEFT JOIN EMPLOYEE e2 
                ON e1.DEPARTMENT = e2.DEPARTMENT
               AND e2.DOB > e1.DOB
            GROUP BY 
                e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME
            ORDER BY e1.EMP_ID DESC;
        """;

        // Step 3: Submit SQL query to webhook
        webClient.post()
                .uri(webhookUrl)
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"finalQuery\": \"" + finalQuery.replace("\"", "\\\"") + "\"}")
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(result -> System.out.println("✅ Response: " + result))
                .block();
    }
}
