package com.bapocalypse.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

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

    public String postArticle(Jedis conn, String user, String title, String link) {
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
        if (conn.sadd("voted:" + articleId, user) == 1) {
            conn.zincrby("score:", VOTE_SCORE, article);
            conn.hincrBy(article, "votes", 1L);
        }
    }

    public List<Map<String, String>> getArticles(Jedis conn, int page) {
        return getArticles(conn, page, "score:");
    }

    private List<Map<String, String>> getArticles(Jedis conn, int page, String order) {
        int start = (page - 1) * ARTICLES_PRE_PAGE;           //设置获取文章的起始索引
        int end = start + ARTICLES_PRE_PAGE - 1;              //设置获取文章的结束索引
        Set<String> ids = conn.zrevrange(order, start, end);  //获取多个文章ID
        List<Map<String, String>> articles = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> articleData = conn.hgetAll(id);  //根据文章ID获取文章的详细信息
            articleData.put("id", id);
            articles.add(articleData);
        }
        return articles;
    }

    public void addGroups(Jedis conn, String articleId, String[] toAdd) {
        String article = "article:" + articleId;
        for (String group : toAdd) {
            conn.sadd("group:" + group, article);      //一个文章可能会属于多个群组
        }
    }

    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page) {
        return getGroupArticles(conn, group, page, "score:");
    }

    private List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page, String order) {
        String key = order + group;   //为每个群组的每种排列顺序都创建一个键
        if (!conn.exists(key)) {       //检查是否有已缓存的排序结果，如果没有就进行排序
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order); //对给定的集合集合执行交集运算
            conn.expire(key, 60);   //让Redis在60秒之后自动删除这个有序集合
        }
        return getArticles(conn, page, key);   //进行分页并获取文章数据
    }

    public void printArticles(List<Map<String, String>> articles) {
        for (Map<String, String> article : articles) {
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String, String> entry : article.entrySet()) {
                if (entry.getKey().equals("id")) {
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
