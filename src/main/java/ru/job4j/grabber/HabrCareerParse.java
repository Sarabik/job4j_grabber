package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    private static final int PAGE_COUNT = 5;

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) {
        HabrCareerParse careerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> list = careerParse.list(PAGE_LINK);
        list.forEach(System.out::println);
    }

    private static String retrieveDescription(String link) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        try {
            Connection connection = Jsoup.connect(link);
            Document document = connection.get();
            Elements descriptionText = document.select(".style-ugc");
            descriptionText.forEach(text -> text.children()
                    .forEach(line -> joiner.add(line.text())));
        } catch (IOException e) {
            throw new IllegalArgumentException("Link does not exist");
        }
        return joiner.toString();
    }

    @Override
    public List<Post> list(String link) {
        List<Post> listPosts = new ArrayList<>();
        try {
            for (int page = 1; page <= PAGE_COUNT; page++) {
                Connection connection = Jsoup.connect(String.format("%s?page=%d", link, page));
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> {
                    Element titleElement = row.select(".vacancy-card__title").first();
                    Element linkElement = titleElement.child(0);
                    Element dateElement = row.select(".vacancy-card__date").first().child(0);
                    listPosts.add(parsePost(titleElement, linkElement, dateElement));
                });
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Link does not exist");
        }
        return listPosts;
    }

    private Post parsePost(Element titleEl, Element linkEl, Element dateEl) {
        String vacancyName = titleEl.text();
        String pageLink = String.format("%s%s", SOURCE_LINK, linkEl.attr("href"));
        LocalDateTime date = dateTimeParser.parse(dateEl.attr("datetime"));
        String description = retrieveDescription(pageLink);
        return new Post(vacancyName, pageLink, description, date);
    }
}
