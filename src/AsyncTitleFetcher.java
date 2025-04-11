import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyncTitleFetcher {

    public static void completableFuture() throws Exception {
        var startTime  = System.currentTimeMillis();

        HttpClient client = HttpClient.newHttpClient();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        List<CompletableFuture<String>> futures = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("resources/urls.txt"))) {
            String line;
            while ((line = br.readLine()) != null && !line.isBlank()) {
                String url = line;
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    HttpRequest request = HttpRequest.newBuilder()
                                                     .uri(URI.create(url))
                                                     .header("User-Agent", "Mozilla/5.0")
                                                     .GET()
                                                     .build();

                    try {
                        HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
                        Matcher matcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE)
                                                 .matcher(res.body());
                        String title = matcher.find() ? matcher.group(1) : "Title not found";
                        return Thread.currentThread().getName() + " → " + url + " → Title: " + title;
                    } catch (IOException | InterruptedException e) {
                        return "Error fetching " + url + ": " + e.getMessage();
                    }
                }, executorService);
                futures.add(future);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Wait for all tasks and print the result
        CompletableFuture<Void> allDone = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]));

        allDone.thenRun(() -> futures.forEach(f -> {
            try {
                System.out.println(f.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        })).join(); // Wait for completion
        var endTime  = System.currentTimeMillis();
        System.out.println("time taken to execute completable future: "+(endTime - startTime));
    }

    public void future() throws IOException {
        var startTime  = System.currentTimeMillis();
        HttpClient client = HttpClient.newHttpClient();

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        BufferedReader br = new BufferedReader(new FileReader("resources/urls.txt"));
        String line = null;
        List<Future<String>> futures = new ArrayList<>();
        while (Objects.nonNull(line = br.readLine()) && !line.isBlank()) {
            String url = line;
            Future<String> future = executorService.submit(() -> {

                HttpRequest request = HttpRequest.newBuilder()
                                                 .uri(URI.create(url))
                                                 .header("User-Agent", "Mozilla/5.0")
                                                 .GET()
                                                 .build();
                try {
                    HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());

                    Pattern pattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(res.body());
                    String title;
                    title = matcher.find() ? matcher.group(1) : "Title not found";
                    return Thread.currentThread().getName() + " → " + url + " → Title: " + title;
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        executorService.shutdown();
        br.close();

        for (Future<String> future : futures) {
            try {
                System.out.println(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        var endTime  = System.currentTimeMillis();
        System.out.println("time taken to execute completable future: "+(endTime - startTime));
    }
}
