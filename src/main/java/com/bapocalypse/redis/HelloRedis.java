package com.bapocalypse.redis;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**
 * @package: com.bapocalypse.redis
 * @Author: 陈淼
 * @Date: 2016/12/6
 * @Description: 构建一个文章投票网站
 */
public class HelloRedis {
    private final static int ONE_WEEK_IN_SECONDS = 7 * 86400;  //一周的秒数
    private final static int VOTE_SCORE = 432;                 //文章每获得一支持票，就增加432分
    private final static int ARTICLES_PRE_PAGE = 25;           //获取25篇文章

    public void run() {
        Jedis conn = new Jedis("localhost");
        //conn.select用于切换到指定的数据库，数据库索引号index用数字值指定，以0作为起始索引值
        conn.select(5);
    }

    public String postArticle(Jedis conn, String user, String title, String link){
        //生成一个新的文章ID,conn.incr()作用为将键存储的值加上1。
        String articleId = String.valueOf(conn.incr("article:"));
        String voted = "voted:" + articleId;
        //将发布文章的用户添加到文章的已投票用户名单
        conn.sadd(voted, user);
        //设置这个名单的过期时间设置为一周
        conn.expire(voted, ONE_WEEK_IN_SECONDS);
        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        Map<String, String> articleData = new HashMap<>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("poster", user);
        articleData.put("date", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);
        //将文章添加到根据发布时间排序的有序集合和根据评分排序的有序集合里面
        conn.zadd("time:", now, article);
        conn.zadd("score:", now + VOTE_SCORE, article);
        return articleId;
    }


    public void articleVote(Jedis conn, String user, String article) {
        //获得系统的时间，单位为毫秒,转换为秒，并减去一周的秒数，计算文章的投票截止时间
        long cutoff = System.currentTimeMillis() / 1000 - ONE_WEEK_IN_SECONDS;
        //检查是否还可以对文章进行投票
        if (conn.zscore("time:", article) < cutoff) return;

        //从article:id标识符里面取出文章ID
        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1){
            conn.zincrby("score:" , VOTE_SCORE , article);
            conn.hincrBy(article, "votes", 1L);
        }
    }

}
