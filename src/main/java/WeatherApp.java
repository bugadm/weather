import com.google.gson.JsonArray;
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
            System.out.println("Ошибка: Не найден файл config.properties. Создайте его в корне проекта.");
            return;
        }

        String apiKey = properties.getProperty("yandex.api.key");
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("ВСТАВЬ_СЮДА_СВОЙ_КЛЮЧ_ОТ_ЯНДЕКСА")) {
            System.out.println("Ошибка: Впишите реальный ключ в config.properties!");
            return;
        }

        double lat = 55.75; 
        double lon = 37.62;
        int limit = 3;

        try {
            String urlString = String.format(
                    "https://api.weather.yandex.ru/v2/forecast?lat=%s&lon=%s&limit=%d",
                    lat, lon, limit
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("X-Yandex-Weather-Key", apiKey)
                    .GET()
                    .build();

            System.out.println("Отправляем запрос к API Яндекс.Погоды (v2)...");
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Ошибка HTTP. Код ответа: " + response.statusCode());
                System.out.println("Ответ сервера: " + response.body());
                return;
            }

            String jsonResponse = response.body();

            System.out.println("\n=== ПОЛНЫЙ JSON ОТВЕТ ===");
            System.out.println(jsonResponse);
            System.out.println("=========================\n");

            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

            JsonObject fact = root.getAsJsonObject("fact");
            double currentTemp = fact.get("temp").getAsDouble();
            System.out.println("Текущая температура (fact.temp): " + currentTemp + "°C");

            JsonArray forecasts = root.getAsJsonArray("forecasts");
            double sumTempAvg = 0;
            int count = 0;

            int daysToProcess = Math.min(limit, forecasts.size());

            for (int i = 0; i < daysToProcess; i++) {
                JsonObject forecastDay = forecasts.get(i).getAsJsonObject();
                JsonObject parts = forecastDay.getAsJsonObject("parts");
                JsonObject dayPart = parts.getAsJsonObject("day");
                
                if (dayPart != null && dayPart.has("temp_avg")) {
                    double tempAvg = dayPart.get("temp_avg").getAsDouble();
                    sumTempAvg += tempAvg;
                    count++;
                }
            }

            if (count > 0) {
                double averageTemp = sumTempAvg / count;
                System.out.printf("Средняя температура за %d дн.: %.1f°C%n", count, averageTemp);
            } else {
                System.out.println("Не удалось извлечь temp_avg из прогноза.");
            }

        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}