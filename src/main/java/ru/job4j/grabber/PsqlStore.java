package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        try {
            Connection cnn = DriverManager.getConnection(
                    cfg.getProperty("jdbc.url"),
                    cfg.getProperty("jdbc.username"),
                    cfg.getProperty("jdbc.password"));
            this.cnn = cnn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement st = cnn.prepareStatement(
                "INSERT INTO post (name, text, link, created) VALUES (?, ?, ?, ?) ON CONFLICT (link) DO NOTHING",
                Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, post.getTitle());
            st.setString(2, post.getDescription());
            st.setString(3, post.getLink());
            st.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            st.executeUpdate();
            try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try (Statement st = cnn.createStatement()) {
            ResultSet result = st.executeQuery("SELECT * FROM post;");
            while (result.next()) {
                list.add(getPostFromSet(result));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement st = cnn.prepareStatement(
                "SELECT * FROM post WHERE id = ?")) {
            st.setInt(1, id);
            ResultSet result = st.executeQuery();
            if (result.next()) {
                post = getPostFromSet(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

    private Post getPostFromSet(ResultSet result) throws SQLException {
        return new Post(
                result.getInt("id"),
                result.getString("name"),
                result.getString("text"),
                result.getString("link"),
                result.getTimestamp("created").toLocalDateTime());
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        Properties cfg = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("grabber.properties")) {
            cfg.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Post testPost1 = new Post("Java Developer1", "link1", "description", LocalDateTime.now());
        Post testPost2 = new Post("Java Developer2", "link2", "description", LocalDateTime.now());
        try (PsqlStore store = new PsqlStore(cfg)) {
            store.save(testPost1);
            store.save(testPost2);
            store.getAll().forEach(System.out::println);
            System.out.println(store.findById(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
