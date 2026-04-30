package com.prathamesh.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {
    @Value("${rate.limit.max-requests}")
    private int maxRequests;
    @Value("${rate.limit.window-size}")
    private long windowSize;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private RestTemplate restTemplate;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String key = "rate_limit:"+ip;
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(key,0,now-windowSize);
        Long count = redisTemplate.opsForZSet().zCard(key);
        if(count!=null && count>=maxRequests){
            response.setStatus(429);
            return;
        }
        redisTemplate.opsForZSet().add(key,String.valueOf(now),now);
        redisTemplate.expire(key,windowSize, TimeUnit.MILLISECONDS);
        String result = restTemplate.getForObject("https://jsonplaceholder.typicode.com/posts",String.class);
        response.setContentType("application/json");
        if(result != null)
            response.getWriter().write(result);
        else
            response.setStatus(502);
    }
}
