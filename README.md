# 🤖 AI Code Review Bot

An intelligent, automated code review system built with **Spring Boot**, **Ollama (Llama 3.2)**, and **pgvector RAG**. Automatically reviews GitHub Pull Requests with full project context — no human intervention needed.

---

## 🏗️ Architecture

```
Developer opens PR
        ↓
GitHub Webhook (HMAC validated)
        ↓
Spring Boot (WebhookController)
        ↓
Async Review Pipeline (ReviewService)
        ↓
RAG: pgvector similarity search
(finds relevant project files)
        ↓
Ollama Llama 3.2
(reviews diff + project context)
        ↓
Comments posted back to GitHub PR
```

---

## ✨ Features

- **Automatic PR Reviews** — triggered instantly when a PR is opened, updated, or reopened
- **RAG-Powered Context** — indexes your entire codebase into pgvector, injects the most relevant files into every review prompt
- **HMAC Security** — validates every GitHub webhook signature before processing
- **Severity Levels** — flags issues as 🔴 error, 🟡 warning, or 🔵 info
- **Inline Comments** — posts comments directly on the exact line in the PR
- **Project Conventions** — reads `CLAUDE_CONTEXT.md` to understand your coding standards
- **Async Processing** — returns 200 immediately, reviews run in background
- **Local AI** — runs entirely on your machine with Ollama, zero API costs

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Java 21 |
| AI Model | Ollama — Llama 3.2 (chat), nomic-embed-text (embeddings) |
| Vector DB | PostgreSQL + pgvector 0.8.5 |
| GitHub Integration | GitHub Webhooks, GitHub REST API |
| Async | Spring @Async, ApplicationEvents |
| Local Tunnel | Smee.io (development) |

---

## 🚀 Getting Started

### Prerequisites

- Java 21
- Docker Desktop
- Ollama installed (`brew install ollama`)
- GitHub account with webhook access

### 1. Clone the repository

```bash
git clone https://github.com/Yogeshtalreja/Automated_PR_Review.git
```

### 2. Start pgvector

```bash
docker-compose up -d
```

### 3. Pull Ollama models

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

### 4. Configure properties

Create `src/main/resources/application-local.properties`:

```properties
github.token=ghp_your_github_token
github.api.base-url=https://api.github.com
github.webhook.secret=your_webhook_secret
ollama.base-url=http://localhost:11434
ollama.model=llama3.2
ollama.embedding-model=nomic-embed-text
```

### 5. Add project context

Create `CLAUDE_CONTEXT.md` in the project root describing your architecture, conventions, and tech stack. The AI uses this to give better reviews.

### 6. Start the bot

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 7. Set up Smee (local development)

```bash
npm install -g smee-client
smee --url https://smee.io/YOUR_CHANNEL --target http://localhost:8080/webhook
```

### 8. Index your codebase

```bash
curl -X POST "http://localhost:8080/index?repoName=owner/repo&localPath=/path/to/repo"
```

### 9. Add GitHub webhook

Go to your repo → Settings → Webhooks → Add webhook:
- Payload URL: your Smee URL
- Content type: `application/json`
- Secret: same as `github.webhook.secret`
- Events: Pull requests only

---

## 📡 API Endpoints

### Manual Review

```
GET /review
```

Manually trigger a review for any PR.

**Parameters:**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `owner` | string | ✅ | — | GitHub repo owner |
| `repo` | string | ✅ | — | Repository name |
| `pr` | int | ✅ | — | Pull request number |
| `minSeverity` | string | ❌ | `info` | Minimum severity: `info`, `warning`, `error` |

**Example:**

```bash
curl "http://localhost:8080/review?owner=Yogeshtalreja&repo=Email-Verification-Using-RestAPI-Spring&pr=1&minSeverity=warning"
```

**Response:**

```json
{
  "pr": 1,
  "repo": "owner/repo",
  "minSeverity": "warning",
  "filesReviewed": 3,
  "totalComments": 7,
  "files": [
    {
      "filename": "src/main/java/AppUserService.java",
      "status": "modified",
      "additions": 5,
      "deletions": 2,
      "contextFiles": [
        "src/main/java/AppUser.java",
        "src/main/java/AppUserRepository.java"
      ],
      "comments": [
        {
          "line": 42,
          "severity": "warning",
          "comment": "Missing @Transactional on method that writes to DB"
        }
      ]
    }
  ]
}
```

---

### Index Codebase

```
POST /index
```

Index a local repository into pgvector for RAG-powered reviews.

**Parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `repoName` | string | ✅ | Full repo name e.g. `owner/repo` |
| `localPath` | string | ✅ | Absolute path to cloned repo |

**Example:**

```bash
curl -X POST "http://localhost:8080/index?repoName=owner/repo&localPath=/tmp/my-repo"
```

**Response:**

```
Indexing started for: owner/repo
```

Indexing runs in background — check logs for progress.

---

### Webhook Receiver

```
POST /webhook
```

Receives GitHub webhook events. Called automatically by GitHub — not for manual use.

Validates `X-Hub-Signature-256` HMAC header before processing. Returns `401` if signature is invalid.

---

## 🧠 How RAG Works

1. **Indexing** — every `.java`, `.xml`, `.yml` file in your repo is read, embedded via `nomic-embed-text`, and stored in pgvector
2. **At review time** — the changed file's diff is embedded and compared against all stored embeddings using cosine similarity (`<=>` operator)
3. **Top 5 most relevant files** are fetched and injected into the review prompt
4. **Result** — Ollama sees the diff AND the related classes, giving far more accurate and project-aware reviews

---

## 📁 Project Structure

```
src/main/java/com/pr/review/reviewbot/
├── WebhookController.java       # Receives GitHub webhooks
├── ReviewController.java        # Manual review endpoint
├── ReviewService.java           # Core async review pipeline
├── CommentMapper.java           # Maps AI comments to GitHub format
├── config/
│   ├── GitHubProperties.java    # GitHub config binding
│   ├── OllamaProperties.java    # Ollama config binding
│   ├── WebClientConfig.java     # WebClient beans
│   └── HmacValidator.java       # Webhook signature validation
├── github/
│   ├── GitHubClient.java        # GitHub REST API client
│   ├── PullRequestFile.java     # PR file DTO
│   ├── PullRequestEvent.java    # Internal event object
│   └── GitHubReviewRequest.java # GitHub review API DTO
├── ollama/
│   ├── OllamaClient.java        # Ollama API client
│   ├── PromptBuilder.java       # Builds review prompts
│   ├── ReviewComment.java       # Parsed AI comment
│   ├── OllamaChatRequest.java   # Ollama chat DTO
│   ├── OllamaChatResponse.java  # Ollama response DTO
│   ├── EmbeddingRequest.java    # Embedding request DTO
│   └── EmbeddingResponse.java   # Embedding response DTO
├── diff/
│   ├── DiffParser.java          # Parses unified diff format
│   └── DiffLine.java            # Parsed diff line record
└── rag/
    ├── CodeEmbedding.java       # JPA entity for embeddings
    ├── CodeEmbeddingRepository  # Spring Data repository
    ├── CodeIndexer.java         # Walks and indexes codebase
    ├── RagService.java          # Similarity search + context
    └── IndexController.java     # Triggers indexing via HTTP
```

---

## 🔒 Security

- All webhook requests validated with HMAC-SHA256 before processing
- GitHub token stored in gitignored local properties file
- Webhook secret never committed to version control
- Timing-safe signature comparison using `MessageDigest.isEqual`

---
