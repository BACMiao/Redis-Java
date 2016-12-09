package com.bapocalypse.redis;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @package: com.bapocalypse.redis
 * @Author: 陈淼
 * @Date: 2016/12/7
 * @Description: 投票功能的测试类
 */
public class HelloRedisTest {

    @Test
    public void testPostArticle() throws Exception {
        HelloRedis hello = new HelloRedis();
        Jedis conn = new Jedis("localhost");
        conn.select(5);
        String articleId = hello.postArticle(conn, "username", "A title", "http://www.google.com");
        System.out.println("We posted a new article with id: " + articleId);
        System.out.println("Its HASH looks like:");
        //获取散列包含的所有键值对
        Map<String, String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry entry : articleData.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println();
    }

    @Test
    public void testArticleVote() throws Exception {
        HelloRedis hello = new HelloRedis();
        Jedis conn = new Jedis("localhost");
        conn.select(5);
        hello.articleVote(conn, "cm", "article:4");
        String votes = conn.hget("article:4", "votes");
        System.out.println("We voted for the article, it now has votes: " + votes);
    }

    @Test
    public void testGetArticles() throws Exception {
        HelloRedis hello = new HelloRedis();
        Jedis conn = new Jedis("localhost");
        conn.select(5);
        System.out.println("The currently highest-scoring articles are:");
        List<Map<String, String>> articles = hello.getArticles(conn, 1);
        hello.printArticles(articles);
    }

    @Test
    public void testAddGroups() throws Exception {
        HelloRedis hello = new HelloRedis();
        Jedis conn = new Jedis("localhost");
        conn.select(5);
        hello.addGroups(conn, "2", new String[]{"new-group"});
        System.out.println("We added the article to a new group, other articles include:");
        List<Map<String, String>> articles = hello.getGroupArticles(conn, "new-group", 1);
        hello.printArticles(articles);
    }
}