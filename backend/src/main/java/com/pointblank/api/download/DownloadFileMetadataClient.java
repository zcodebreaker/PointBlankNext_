package com.pointblank.api.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DownloadFileMetadataClient {

    private static final int MAX_REDIRECTS = 5;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern CONTENT_RANGE_TOTAL = Pattern.compile("^bytes\\s+\\d+-\\d+/(\\d+)$", Pattern.CASE_INSENSITIVE);
    private final HttpClient httpClient;

    public DownloadFileMetadataClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    DownloadFileMetadataClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public DownloadFileMetadata inspect(String rawUrl) {
        URI uri = validatePublicHttpUri(rawUrl);

        try {
            HttpResponse<Void> headResponse = followHeadRedirects(uri);
            long size = headResponse.statusCode() == 405 || headResponse.statusCode() == 501
                    ? -1L
                    : contentLength(headResponse);
            OffsetDateTime updatedAt = lastModified(headResponse).orElse(null);

            if (size < 0) {
                HttpResponse<InputStream> rangeResponse = followRangeRedirects(uri);
                try (InputStream ignored = rangeResponse.body()) {
                    size = rangeTotal(rangeResponse);
                    if (updatedAt == null) {
                        updatedAt = lastModified(rangeResponse).orElse(null);
                    }
                }
            }

            if (size < 0) {
                throw new DownloadSettingsException(
                        HttpStatus.BAD_GATEWAY,
                        "O servidor do arquivo não informou o tamanho do download");
            }

            return new DownloadFileMetadata(
                    size,
                    updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt);
        } catch (DownloadSettingsException exception) {
            throw exception;
        } catch (IOException exception) {
            throw unavailable("Não foi possível consultar o arquivo informado", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("A consulta ao arquivo foi interrompida", exception);
        }
    }

    private HttpResponse<Void> followHeadRedirects(URI initialUri) throws IOException, InterruptedException {
        URI uri = initialUri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpRequest request = request(uri).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (!isRedirect(response.statusCode())) {
                if (response.statusCode() == 405 || response.statusCode() == 501) {
                    return response;
                }
                requireSuccessful(response.statusCode());
                return response;
            }
            uri = redirectedUri(uri, response);
        }
        throw new DownloadSettingsException(HttpStatus.BAD_GATEWAY, "O arquivo possui redirecionamentos demais");
    }

    private HttpResponse<InputStream> followRangeRedirects(URI initialUri) throws IOException, InterruptedException {
        URI uri = initialUri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpRequest request = request(uri)
                    .header("Range", "bytes=0-0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (!isRedirect(response.statusCode())) {
                requireSuccessful(response.statusCode());
                return response;
            }
            response.body().close();
            uri = redirectedUri(uri, response);
        }
        throw new DownloadSettingsException(HttpStatus.BAD_GATEWAY, "O arquivo possui redirecionamentos demais");
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "*/*")
                .header("User-Agent", "PointBlank-Download-Metadata/1.0");
    }

    private URI redirectedUri(URI currentUri, HttpResponse<?> response) {
        String location = response.headers().firstValue("Location")
                .orElseThrow(() -> new DownloadSettingsException(
                        HttpStatus.BAD_GATEWAY,
                        "O servidor respondeu com um redirecionamento inválido"));
        return validatePublicHttpUri(currentUri.resolve(location).toString());
    }

    private URI validatePublicHttpUri(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw invalidUrl();
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (host.equals("localhost") || host.endsWith(".localhost")) {
                throw invalidUrl();
            }
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (!isPublicAddress(address)) {
                    throw invalidUrl();
                }
            }
            return uri;
        } catch (URISyntaxException | UnknownHostException exception) {
            throw invalidUrl();
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return bytes.length != 16 || (bytes[0] & 0xfe) != 0xfc;
        }

        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        return !(first == 0
                || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 198 && (second == 18 || second == 19)));
    }

    private long contentLength(HttpResponse<?> response) {
        return response.headers().firstValueAsLong("Content-Length").orElse(-1L);
    }

    private long rangeTotal(HttpResponse<?> response) {
        Optional<String> contentRange = response.headers().firstValue("Content-Range");
        if (contentRange.isPresent()) {
            Matcher matcher = CONTENT_RANGE_TOTAL.matcher(contentRange.get().trim());
            if (matcher.matches()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        return contentLength(response);
    }

    private Optional<OffsetDateTime> lastModified(HttpResponse<?> response) {
        return response.headers().firstValue("Last-Modified").flatMap(value -> {
            try {
                return Optional.of(ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toOffsetDateTime());
            } catch (DateTimeParseException exception) {
                return Optional.empty();
            }
        });
    }

    private void requireSuccessful(int statusCode) {
        if (statusCode == 404) {
            throw new DownloadSettingsException(
                    HttpStatus.BAD_REQUEST,
                    "O arquivo não foi encontrado. Verifique se a URL aponta diretamente para um arquivo público");
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new DownloadSettingsException(
                    HttpStatus.BAD_GATEWAY,
                    "O servidor do arquivo respondeu com HTTP " + statusCode);
        }
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private DownloadSettingsException invalidUrl() {
        return new DownloadSettingsException(
                HttpStatus.BAD_REQUEST,
                "Informe uma URL pública HTTP ou HTTPS válida");
    }

    private DownloadSettingsException unavailable(String message, Exception cause) {
        return new DownloadSettingsException(HttpStatus.BAD_GATEWAY, message + ": " + cause.getMessage());
    }
}
