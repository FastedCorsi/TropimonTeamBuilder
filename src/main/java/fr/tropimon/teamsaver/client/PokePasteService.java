package fr.tropimon.teamsaver.client;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.util.List;

/** Fetch only the explicit public Poképaste URL pasted by the user, never arbitrary hosts. */
final class PokePasteService {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private PokePasteService() { }

    static URI rawUri(String raw) {
        try {
            URI uri = URI.create(raw.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"pokepast.es".equalsIgnoreCase(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || !uri.getPath().matches("/[0-9a-fA-F]{16}(?:/raw)?/?")) {
                throw new IllegalArgumentException("invalid_url");
            }
            return URI.create("https://pokepast.es/" + uri.getPath().split("/")[1] + "/raw");
        } catch (RuntimeException exception) { throw new IllegalArgumentException("invalid_url"); }
    }

    static CompletableFuture<ShowdownPaste.Parsed> load(String text) {
        String stripped = text == null ? "" : text.strip();
        if (!stripped.startsWith("https://") && !stripped.startsWith("http://")) {
            return CompletableFuture.completedFuture(ShowdownPaste.parse(stripped));
        }
        URI uri = rawUri(stripped);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                .header("Accept", "text/plain").GET().build();
        BoundedBody body = new BoundedBody();
        CompletableFuture<HttpResponse<byte[]>> network = HTTP.sendAsync(request, ignored -> body);
        CompletableFuture<ShowdownPaste.Parsed> parsed = network.thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalArgumentException("network");
            return ShowdownPaste.parse(new String(response.body(), StandardCharsets.UTF_8));
        }).orTimeout(20, TimeUnit.SECONDS);
        parsed.whenComplete((result, failure) -> {
            if (failure != null) {
                network.cancel(true);
                body.cancel();
            }
        });
        return parsed;
    }

    static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        @Override public CompletionStage<byte[]> getBody() { return result; }
        @Override public synchronized void onSubscribe(Flow.Subscription value) {
            subscription = value;
            if (result.isDone()) value.cancel();
            else value.request(1);
        }
        @Override public synchronized void onNext(List<ByteBuffer> buffers) {
            if (result.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > ShowdownPaste.MAX_LENGTH - bytes.size()) {
                    result.completeExceptionally(new IllegalArgumentException("too_large"));
                    subscription.cancel();
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable failure) { result.completeExceptionally(failure); }
        @Override public synchronized void onComplete() { result.complete(bytes.toByteArray()); }
        synchronized void cancel() {
            result.cancel(true);
            if (subscription != null) subscription.cancel();
        }
    }
}
