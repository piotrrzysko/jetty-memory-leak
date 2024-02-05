package com.github.jettybug;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class Jetty11Main {

    public static void main(String[] args) throws Exception {
        WireMockServer wireMockServer = new WireMockServer(8080);
        wireMockServer.start();

        Duration idleTimeout = Duration.ofMillis(1000);
        Duration timeout = Duration.ofMillis(1000);
        Duration connectionTimeout = Duration.ofSeconds(15);

        ExecutorService executor = Executors.newFixedThreadPool(30);
        HttpClientTransportOverHTTP transport = new HttpClientTransportOverHTTP(new ClientConnector());
        HttpClient client = new HttpClient(transport);
        client.setExecutor(executor);
        client.setMaxConnectionsPerDestination(100);
        client.setMaxRequestsQueuedPerDestination(100);
        client.setCookieStore(new HttpCookieStore.Empty());
        client.setIdleTimeout(idleTimeout.toMillis());
        client.setFollowRedirects(false);
        client.setConnectTimeout(connectionTimeout.toMillis());
        client.start();

        wireMockServer.stubFor(post(urlEqualTo("/delayed")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000)));

        for (int i = 0; i < 10000; i++) {
            Request baseRequest = client.newRequest(wireMockServer.baseUrl() + "/delayed")
                    .method(HttpMethod.POST)
                    .timeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .idleTimeout(idleTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .body(new BytesRequestContent(new byte[0]));
            try {
                printQueueSize(client.getScheduler());
                baseRequest.send();
                throw new RuntimeException("Request was expected to time out");
            } catch (TimeoutException e) {
                // expected
            }
        }
    }

    private static void printQueueSize(Scheduler scheduler) {
        if (scheduler instanceof ScheduledExecutorScheduler) {
            try {
                ScheduledExecutorScheduler scheduledExecutorScheduler = (ScheduledExecutorScheduler) scheduler;
                Field f = scheduledExecutorScheduler.getClass().getDeclaredField("scheduler");
                f.setAccessible(true);
                ScheduledThreadPoolExecutor threadPoolExecutor = (ScheduledThreadPoolExecutor) f.get(scheduledExecutorScheduler);
                System.out.println("Queue size: " + threadPoolExecutor.getQueue().size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
