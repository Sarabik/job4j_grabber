package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.sql.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DriverManager;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {

    public static void main(String[] args) {
        Properties pr = getProperties();
        try (Connection connection = initConnection(pr)) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format(
                        "create table if not exists rabbit(%s, %s);",
                        "id serial primary key",
                        "created_date timestamp"
                );
                statement.execute(sql);
            }
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            JobDataMap data = new JobDataMap();
            data.put("connection", connection);

            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(Integer.parseInt(pr.getProperty("rabbit.interval")))
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            printTable(connection, "rabbit");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static Properties getProperties() {
        Properties pr = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            pr.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pr;
    }

    private static Connection initConnection(Properties pr) {
        Connection connection = null;
        try {
            Class.forName(pr.getProperty("driver_class"));
            String url = pr.getProperty("url");
            String login = pr.getProperty("login");
            String password = pr.getProperty("password");
            connection =  DriverManager.getConnection(url, login, password);
            return connection;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return connection;
    }

    public static void printTable(Connection connection, String tableName) throws Exception {
        try (Statement statement = connection.createStatement()) {
            ResultSet selection = statement.executeQuery(String.format(
                    "select * from %s", tableName
            ));
            System.out.println("| id | created_date |");
            while (selection.next()) {
                int id = selection.getInt("id");
                Timestamp time = selection.getTimestamp("created_date");
                System.out.println(String.format("| %s | %s |", id, time));
            }
        }
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connection");
            try (Statement statement = connection.createStatement()) {
                String sql = String.format(
                        "insert into rabbit (created_date) values (%s);",
                        "current_timestamp"
                );
                statement.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
