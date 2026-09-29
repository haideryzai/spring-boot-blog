# Blog App

A REST API for a blog, built with Spring Boot 3 and Java 17. Users register and log in with JWT, write posts, file them under categories, and comment on each other's posts.

## Tech stack

- **Spring Boot 3.4**: Web, Data JPA, Validation, Security
- **PostgreSQL** in dev/prod, **H2** (in-memory) for tests
- **JWT** auth via [jjwt](https://github.com/jwtk/jjwt), passwords hashed with BCrypt
- **Docker** + Docker Compose

## Project structure

```
src/main/java/com/blogapp/blogapp
├── config/         SecurityConfig (URL rules, JWT filter), AdminInitializer (seeds admin)
├── controllers/    REST endpoints: Auth, User, Post, Comment, Category
├── dto/            Request/response records; entities are never returned directly
├── entity/         JPA entities: User, Post, Comment, Category, Role
├── exception/      Custom exceptions + GlobalExceptionHandler (JSON error responses)
├── repository/     Spring Data JPA repositories
├── security/       JwtService, JwtAuthenticationFilter, SecurityUser, CustomUserDetailsService
└── service/        Business logic and ownership checks
```

### Data model

```
User 1───* Post *───0..1 Category
  │          │
  │          1
  │          │
  └───1───*  Comment
```

- Deleting a **user** deletes their posts and comments.
- Deleting a **post** deletes its comments.
- Deleting a **category** keeps its posts and sets their category to `null`.

## Getting started

### 1. Configure the environment

All configuration comes from environment variables. The app reads a `.env` file in the project root (via `spring.config.import`), and Docker Compose reads the same file.

```bash
cp .env.example .env
# Generate a JWT secret and put it in .env as JWT_SECRET
openssl rand -base64 32
```

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `blogapp` | PostgreSQL location |
| `DB_USERNAME` / `DB_PASSWORD` | `blogapp` / `blogapp` | PostgreSQL credentials |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode |
| `JPA_SHOW_SQL` | `false` | Log SQL statements |
| `JWT_SECRET` | *(required)* | Base64 key, at least 32 bytes |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |
| `ADMIN_NAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | *(empty)* | If `ADMIN_EMAIL` is set, this admin account is created on startup when it doesn't exist yet |

Real environment variables override values in `.env`.

### 2a. Run everything with Docker

```bash
docker compose up --build
```

This starts PostgreSQL (data kept in the `postgres-data` volume) and the app on `http://localhost:8080`.

### 2b. Or run the app locally

Start only the database in Docker, then run the app with Maven:

```bash
docker compose up -d db
./mvnw spring-boot:run
```

### Run the tests

```bash
./mvnw test
```

Tests use an in-memory H2 database (`src/test/resources/application.properties`), so no Postgres is needed.

## Authentication

1. `POST /api/auth/register` or `POST /api/auth/login` returns a token.
2. Send it on protected requests:

```
Authorization: Bearer <token>
```

Roles:
- **USER**: every registered account.
- **ADMIN**: created from `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Manages categories, lists all users, and can edit or delete any post, comment or user.

The token's subject is the user's email, so after changing your email you must log in again.

## API reference

Base URL: `http://localhost:8080`

**Access** column: 🌐 public, 🔑 any logged-in user, 👤 owner or admin, 🛡️ admin only.

### Auth

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | 🌐 | Create an account, returns token |
| POST | `/api/auth/login` | 🌐 | Log in, returns token |

```json
// POST /api/auth/register
{ "name": "Alice", "email": "alice@example.com", "password": "password123" }

// Response 201 (login returns the same shape with 200)
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "user": { "id": 1, "name": "Alice", "email": "alice@example.com", "bio": null, "role": "USER", "createdAt": "2026-09-29T10:00:00Z" }
}
```

Validation: `name` required (max 100), `email` must be valid, `password` 8–100 chars.

### Users

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/users` | 🛡️ | List users (paginated) |
| GET | `/api/users/me` | 🔑 | Current user's profile |
| GET | `/api/users/{id}` | 🔑 | Get a user |
| PUT | `/api/users/{id}` | 👤 | Update profile |
| DELETE | `/api/users/{id}` | 👤 | Delete account, including the user's posts and comments |

```json
// PUT /api/users/1  ("password" is optional; omit it to keep the current one)
{ "name": "Alice B", "email": "alice@example.com", "bio": "I write about Java", "password": "newpassword123" }
```

### Posts

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/posts` | 🌐 | List/search posts (paginated) |
| GET | `/api/posts/{id}` | 🌐 | Get a post |
| POST | `/api/posts` | 🔑 | Create a post |
| PUT | `/api/posts/{id}` | 👤 | Update a post |
| DELETE | `/api/posts/{id}` | 👤 | Delete a post and its comments |

Query parameters for `GET /api/posts` (all optional):

| Param | Example | Description |
|---|---|---|
| `search` | `spring boot` | Case-insensitive match on title or content |
| `categoryId` | `2` | Only posts in this category |
| `authorId` | `1` | Only posts by this user |
| `page`, `size` | `0`, `10` | Pagination (size max 100, default 10) |
| `sort` | `createdAt,desc` | Sort field and direction (default `createdAt,desc`) |

```json
// POST /api/posts  ("categoryId" is optional)
{ "title": "Hello Spring", "content": "My first post", "categoryId": 2 }

// Response 201
{
  "id": 5,
  "title": "Hello Spring",
  "content": "My first post",
  "author": { "id": 1, "name": "Alice" },
  "category": { "id": 2, "name": "Java", "description": "All things Java" },
  "createdAt": "2026-09-29T10:05:00Z",
  "updatedAt": "2026-09-29T10:05:00Z"
}
```

### Comments

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/posts/{postId}/comments` | 🌐 | List comments on a post (paginated, oldest first) |
| POST | `/api/posts/{postId}/comments` | 🔑 | Add a comment |
| PUT | `/api/comments/{id}` | 👤 | Edit a comment |
| DELETE | `/api/comments/{id}` | 👤 | Delete a comment. The post's author may also delete comments on their post |

```json
// POST /api/posts/5/comments
{ "content": "Great post!" }
```

### Categories

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/categories` | 🌐 | List all categories (sorted by name) |
| GET | `/api/categories/{id}` | 🌐 | Get a category |
| POST | `/api/categories` | 🛡️ | Create a category (names are unique, case-insensitive) |
| PUT | `/api/categories/{id}` | 🛡️ | Update a category |
| DELETE | `/api/categories/{id}` | 🛡️ | Delete a category; its posts become uncategorized |

```json
// POST /api/categories
{ "name": "Java", "description": "All things Java" }
```

### Paginated responses

List endpoints (except categories) return:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

### Errors

Errors use the [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) format:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/auth/register",
  "errors": { "email": "must be a well-formed email address" }
}
```

| Status | When |
|---|---|
| 400 | Validation failed, or unknown `sort` field |
| 401 | Missing, invalid or expired token; wrong email/password on login |
| 403 | Logged in but not allowed (not the owner, or not an admin) |
| 404 | Resource doesn't exist |
| 409 | Email or category name already taken |

## Quick try with curl

```bash
# Register and grab the token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","password":"password123"}' | jq -r .token)

# Create a post
curl -s -X POST localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Hello","content":"First post"}'

# List posts (no token needed)
curl -s 'localhost:8080/api/posts?search=hello'
```
