# API Gateway with Rate Limiting

A production-inspired API Gateway built with **Spring Boot** and **Redis** that demonstrates core infrastructure-level backend concepts — request filtering, sliding window rate limiting, and dynamic route proxying.

---

## Architecture

```
Client Request
      │
      ▼
┌─────────────────────────┐
│   RateLimiterFilter     │  ← OncePerRequestFilter (intercepts every request)
│                         │
│  1. Extract client IP   │
│  2. Check Redis Sorted  │
│     Set (sliding window)│
│  3. Allow / Block       │
└────────┬────────────────┘
         │ Allowed
         ▼
┌─────────────────────────┐
│   Route Lookup          │  ← RouteConfig (loaded from application.properties)
│                         │
│  /api/posts   → URL A   │
│  /api/users   → URL B   │
│  /api/comments→ URL C   │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│   RestTemplate Proxy    │  ← Forwards request to downstream service
│                         │
│  Returns response body  │
│  back to client         │
└─────────────────────────┘
```

---

## Features

- **Sliding Window Rate Limiting** — per-IP rate limiting using a Redis Sorted Set. Each request is stored as an entry with a timestamp score. On every request, entries older than the window are pruned and the remaining count is checked against the limit.
- **Dynamic Route Configuration** — routes are defined in `application.properties` and loaded at startup. Adding a new route requires no code changes.
- **Reverse Proxy** — allowed requests are forwarded to the configured downstream URL using `RestTemplate`. The response is streamed back to the client.
- **Automatic Key Expiry** — Redis keys are given a TTL equal to the window size, ensuring no stale data accumulates.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core language |
| Spring Boot 3.5 | Application framework |
| Spring Data Redis | Redis integration |
| Redis 7 | Rate limit state storage |
| Lettuce | Redis client (default with Spring Data Redis) |
| RestTemplate | HTTP proxying to downstream services |
| Docker | Running Redis locally |
| Maven | Build tool |

---

## Project Structure

```
src/main/java/com/prathamesh/apigateway/
├── ApigatewayApplication.java        # Entry point
├── config/
│   ├── RedisConfig.java              # RedisTemplate and RestTemplate beans
│   └── RouteConfig.java              # Loads route mappings from properties
├── controller/
│   └── TestController.java           # Test endpoint
└── filter/
    └── RateLimiterFilter.java        # Core rate limiting and proxying logic
```

---

## How the Rate Limiter Works

This project uses the **Sliding Window** algorithm — a more accurate alternative to Fixed Window.

**Fixed Window flaw:** A user can send 10 requests at 11:59:59 and 10 more at 12:00:01 — 20 requests in 2 seconds — because the window resets at the clock boundary.

**Sliding Window fix:** The window is always "the last N seconds from right now." Redis Sorted Sets make this elegant:

1. Each request is added as an entry with the current timestamp (milliseconds) as its score
2. Before checking the count, all entries older than `now - windowSize` are removed
3. The remaining count is compared against the limit
4. If over the limit → `429 Too Many Requests`
5. A TTL is set on the key equal to the window size to prevent stale keys

---

## Configuration

All tuneable parameters live in `application.properties`:

```properties
# Rate limiting
rate.limit.max-requests=10
rate.limit.window-size=60000

# Route mappings
gateway.routes.posts=https://jsonplaceholder.typicode.com/posts
gateway.routes.users=https://jsonplaceholder.typicode.com/users
gateway.routes.comments=https://jsonplaceholder.typicode.com/comments
```

To add a new route, simply add a new line:
```properties
gateway.routes.todos=https://jsonplaceholder.typicode.com/todos
```

No code changes required.

---

## Running Locally

**Prerequisites:** Java 21, Maven, Docker

```bash
# 1. Clone the repository
git clone https://github.com/Prathamesh-495/apigateway.git
cd apigateway

# 2. Start Redis
docker compose up -d

# 3. Run the application
./mvnw spring-boot:run
```

**Test the gateway:**
```bash
# Route proxying
GET http://localhost:8080/api/posts
GET http://localhost:8080/api/users
GET http://localhost:8080/api/comments

# Rate limiting — send more than 10 requests within 60 seconds
# Returns 429 Too Many Requests after the limit is exceeded

# Unknown route
GET http://localhost:8080/api/xyz  # Returns 404
```

---

## Key Concepts Demonstrated

- **OncePerRequestFilter** — Spring's mechanism for intercepting every HTTP request before it reaches a controller
- **Redis Sorted Sets** — using score-based ordering to implement time-window queries efficiently
- **TTL-based expiry** — letting Redis automatically clean up stale data
- **ConfigurationProperties** — binding a group of properties into a typed Java object
- **Reverse proxying** — forwarding requests to upstream services and returning their responses