package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {

    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";
    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql); ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }
        return results;
    }

    public static void main(String[] args) throws SQLException {

        Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/lx/IdeaProjects/crawler/news", USER_NAME, PASSWORD);


        while (true) {
            //待处理的链接池
            //从数据库加载待处理的链接池
            List<String> linkPool = loadUrlsFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");

            //已经处理的链接池
            //从数据库加载已经处理的链接池

            if (linkPool.isEmpty()) {
                break;
            }
            //arraylist从尾部删除更有效率
            //每次处理完，更新数据库
            //从待处理池子拿一个来处理
            //处理完后从池子和数据库中删除
            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkIntoDatabase(connection, "DELETE FROM LINKS_TO_BE_PROCESSED where link = ?", link);
            //询问数据库，当前链接是否已经处理过
            if (isLinkProcessed(connection, link)) {
                continue;
            }
            //不感兴趣 不处理
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);

                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);

                storeIntoDatabaseIfItIsNewsPage(doc);


                insertLinkIntoDatabase(connection, "INSERT INTO LINKS_ALREADY_PROCESSED (link) values (?)", link);


            }
        }


    }

    private static void insertLinkIntoDatabase(Connection connection, String sql, String link) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, link);
            preparedStatement.executeUpdate();
        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");
            insertLinkIntoDatabase(connection, "insert into LINKS_TO_BE_PROCESSED (link) values (?)", href);
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?")) {
            preparedStatement.setString(1, link);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }


    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            if (link.startsWith("//")) {
                link = "https" + link;
                System.out.println(link);
            }
            HttpGet httpGet = new HttpGet(link);
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");
            System.out.println(link);
            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                System.out.println(response1.getStatusLine());
                HttpEntity entity1 = response1.getEntity();
                String html = EntityUtils.toString(entity1);
                return Jsoup.parse(html);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);

    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

}
