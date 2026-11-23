import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class WeatherApp {

    public static void main(String[] args) {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.out.println("Ошибка: Не найден файл config.properties.");
            return;
        }
        String apiKey = properties.getProperty("yandex.api.key");
        if (apiKey == null || apiKey.equals("ВСТАВЬ_СЮДА_СВОЙ_КЛЮЧ_ОТ_ЯНДЕКСА")) {
            System.out.println("Ошибка: Вы не вписали реальный ключ в config.properties!");
            return;
        }

        String graphqlQuery = """
                {
                  weatherByPoint(request: { lat: 55.75, lon: 37.62 }) {
                    now {
                      temperature
                    }
                    forecast {
                      days(limit: 3) {
                        day {
                          tempAvg
                        }
                      }
                    }
                  }
                }
                """;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.weather.yandex.ru/graphql/query"))
                    .header("X-Yandex-Weather-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"" + graphqlQuery.replace("\"", "\\\"").replace("\n", " ") + "\"}"))
                    .build();

            System.out.println("Отправляем GraphQL-запрос к API Яндекс.Погоды (v3)...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Ошибка HTTP. Код: " + response.statusCode());
                System.out.println(response.body());
                return;
            }

            String jsonResponse = response.body();
            System.out.println("\n=== ПОЛНЫЙ JSON ОТВЕТ (v3) ===");
            System.out.println(jsonResponse);
            System.out.println("=============================\n");

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data").getAsJsonObject("weatherByPoint");

            double currentTemp = data.getAsJsonObject("now").get("temperature").getAsDouble();
            System.out.println("Текущая температура (now.temperature): " + currentTemp + "°C");

            var days = data.getAsJsonObject("forecast").getAsJsonArray("days");
            double sumTempAvg = 0;
            int count = 0;
            for (var dayElement : days) {
                JsonObject day = dayElement.getAsJsonObject().getAsJsonObject("day");
                if (day != null && day.has("tempAvg")) {
                    sumTempAvg += day.get("tempAvg").getAsDouble();
                    count++;
                }
            }
            if (count > 0) {
                double averageTemp = sumTempAvg / count;
                System.out.printf("Средняя температура за %d дн.: %.1f°C%n", count, averageTemp);
            } else {
                System.out.println("Не удалось извлечь tempAvg из прогноза.");
            }

        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}