package ru.job4j.grabber;

import java.util.List;

public interface Store extends AutoCloseable {
    /* save vacancy to db */
    void save(Post post);

    /* get vacancies from db */
    List<Post> getAll();

    /* get vacancy from db by id */
    Post findById(int id);
}
