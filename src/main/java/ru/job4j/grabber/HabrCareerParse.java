package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    private static final int PAGE_COUNT = 5;

    public static void main(String[] args) throws IOException {
        for (int page = 1; page <= PAGE_COUNT; page++) {
            Connection connection = Jsoup.connect(String.format("%s?page=%d", PAGE_LINK, page));
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                Element dateElement = row.select(".vacancy-card__date").first().child(0);
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                HabrCareerDateTimeParser parser = new HabrCareerDateTimeParser();
                LocalDateTime date = parser.parse(dateElement.attr("datetime"));
                System.out.printf("%s %s %s%n", vacancyName, link, date);
            });
        }

    }
}
