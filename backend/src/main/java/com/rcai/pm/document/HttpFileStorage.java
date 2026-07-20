package com.rcai.pm.document;

import com.rcai.pm.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "http")
public class HttpFileStorage implements FileStorage {
    private final String baseUrl;
    private final String bearerToken;
    private final HttpClient client;

    public HttpFileStorage(@Value("${app.storage.http.base-url}") String baseUrl,
                           @Value("${app.storage.http.bearer-token:}") String bearerToken) {
        this(baseUrl, bearerToken, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    HttpFileStorage(String baseUrl, String bearerToken, HttpClient client) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.bearerToken = bearerToken;
        this.client = client;
    }

    @Override
    public String save(MultipartFile file) {
        try {
            String key = UUID.randomUUID() + extension(file.getOriginalFilename());
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(key))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()));
            authorize(request);
            HttpResponse<Void> response = client.send(request.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return key;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "外部文件存储失败");
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "外部文件存储失败");
        }
    }

    @Override
    public Resource load(String storageKey) {
        if (!storageKey.matches("[A-Za-z0-9._-]+")) throw new ApiException(HttpStatus.BAD_REQUEST, "存储键不合法");
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri(storageKey))
                .timeout(Duration.ofSeconds(30)).GET();
            authorize(request);
            HttpResponse<byte[]> response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) throw new ApiException(HttpStatus.NOT_FOUND, "文件不存在");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return new ByteArrayResource(response.body());
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "外部文件读取失败");
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "外部文件读取失败");
        }
    }

    private URI uri(String key) { return URI.create(baseUrl + "/" + key); }
    private void authorize(HttpRequest.Builder request) {
        if (!bearerToken.isBlank()) request.header("Authorization", "Bearer " + bearerToken);
    }
    private String extension(String name) {
        if (name == null) return "";
        int index = name.lastIndexOf('.');
        return index < 0 || name.length() - index > 12 ? "" : name.substring(index).replaceAll("[^A-Za-z0-9.]", "");
    }
}
