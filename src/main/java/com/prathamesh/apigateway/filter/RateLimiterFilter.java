package com.prathamesh.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class RateLimiterFilter extends OncePerRequestFilter {
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Override

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String key = "rate_limit:"+ip;
        long now = System.currentTimeMillis();
        long windowSize=60000;
        int maxRequests=10;
        redisTemplate.opsForZSet().removeRangeByScore(key,0,now-windowSize);
        Long count = redisTemplate.opsForZSet().zCard(key);
        if(count!=null && count>=maxRequests){
            response.setStatus(429);
            return;
        }
        redisTemplate.opsForZSet().add(key,String.valueOf(now),now);
        filterChain.doFilter(request,response);
    }
}
