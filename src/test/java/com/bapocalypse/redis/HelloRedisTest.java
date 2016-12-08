package com.bapocalypse.redis;

import org.junit.Test;
import redis.clients.jedis.Jedis;

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
        Map<String,String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry entry : articleData.entrySet()){
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println();
    }

    @Test
    public void testArticleVote() throws Exception {
        HelloRedis hello = new HelloRedis();
        Jedis conn = new Jedis("localhost");
        conn.select(5);
        hello.articleVote(conn, "cm", "article:1" );
        String votes = conn.hget("article:1", "votes");
        System.out.println("We voted for the article, it now has votes: " + votes);
    }
}