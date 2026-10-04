# Agentic Software Engineering System — URL Shortener

> **Interview Assignment:** Build an Agentic Software Engineering System — URL Shortener  
> **Classification:** Schwab Internal  
> **Prototype Goal:** Demonstrate controlled, governed agentic execution from requirement understanding through release readiness, alongside a working URL shortener service.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Installation and Startup](#installation-and-startup)
7. [URL Shortener Demo](#url-shortener-demo)
8. [Agentic Orchestration API](#agentic-orchestration-api)
9. [Workflow Operations](#workflow-operations)
10. [Reliability Metrics](#reliability-metrics)
11. [Assignment Scenarios](#assignment-scenarios)
12. [Testing Strategy](#testing-strategy)
13. [Engineering Design](#engineering-design)
14. [Security and Compliance](#security-and-compliance)
15. [Failure Handling](#failure-handling)
16. [Trade-offs and Limitations](#trade-offs-and-limitations)
17. [Production Evolution](#production-evolution)
18. [Interview Demo Script](#interview-demo-script)
19. [Useful URLs](#useful-urls)
20. [Project Summary](#project-summary)
21. [License and Interview Use](#license-and-interview-use)
22. [Convenience Scripts](#convenience-scripts)
23. [API Definition](#api-definition)

---

## Overview

This repository provides two related capabilities:

### Working URL Shortener Service

- Create a short URL from an HTTP or HTTPS URL
- Resolve a short URL through a redirect endpoint
- Track click counts
- Query analytics
- Validate input and return consistent errors

### Governed Agentic SDLC Orchestration Prototype

- Accept high-level engineering requirements
- Normalize and decompose requirements into lifecycle stages
- Execute a dependency graph rather than a simple linear chain
- Run testing and documentation in parallel
- Synchronize at the release gate
- Require human approval for high-impact stages
- Enforce bounded retries
- Record rollback and safe-stop events
- Support dynamic re-planning after upstream requirement changes
- Preserve audit/decision lineage and reliability metrics

> **Design principle:** Agent stages are intentionally bounded simulators. They generate reviewable engineering artifacts and decisions rather than executing arbitrary code or shell commands. This demonstrates production-oriented governance without creating an unsafe, unrestricted coding agent.

---

## Architecture

```text
                    +----------------------+
                    |   Browser Demo UI    |
                    +----------+-----------+
                               |
                               v
+----------------+    +----------------------+    +----------------+
| URL Shortener  |--->| Spring Boot REST API |--->| H2 / JPA       |
| Create/Redirect|    +----------+-----------+    +----------------+
| Analytics      |               |
+----------------+               v
                        +----------------------+
                        | Agentic Orchestrator |
                        | Stateful DAG         |
                        +----------+-----------+
                                   |
               +-------------------+-------------------+
               |                   |                   |
               v                   v                   v
        Requirements        Architecture        Implementation
                                                    |
                              +---------------------+------------------+
                              |                                        |
                              v                                        v
                           Testing                              Documentation
                              |                                        |
                              +----------------+-----------------------+
                                               v
                                         Release Gate
                                               |
                                         Human Approval
```

For detailed architecture, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Technology Stack

| Area | Technology | Rationale |
|---|---|---|
| Language | Java 21 | Modern LTS Java, virtual threads, mature ecosystem |
| Framework | Spring Boot 3.5.x | Production-oriented REST/application framework |
| API | Spring Web | REST endpoints and redirect handling |
| Validation | Spring Validation / Jakarta Validation | Boundary validation |
| Persistence | Spring Data JPA | Clean repository abstraction; easy H2/PostgreSQL transition |
| Default database | H2 file database | Zero external setup for the interview demo |
| Tests | JUnit 5, Mockito, MockMvc | Unit and API integration coverage |
| Build | Maven | Standard Java build lifecycle |
| UI | HTML, CSS, JavaScript | No Node/npm dependency for the demo UI |
| Container | Docker | Repeatable local execution |
| CI | GitHub Actions | Build/test automation example |
| Orchestration | Custom stateful DAG in Java | Explicit dependencies, gates, parallel paths, governance |
| Concurrency | Java virtual threads and `ExecutorService` | Simple bounded parallel stage execution |
| API style | JSON REST | Easy to demo with browser, curl, or Postman |

---

## Project Structure

```text
agentic-url-shortener/
├── .github/
│   └── workflows/
│       └── ci.yml
├── docs/
│   ├── ARCHITECTURE.md
│   └── SCENARIOS.md
├── src/
│   ├── main/
│   │   ├── java/com/schwab/agenticurl/
│   │   │   ├── AgenticUrlShortenerApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── MetricsController.java
│   │   │   │   ├── OrchestrationController.java
│   │   │   │   ├── RedirectController.java
│   │   │   │   └── UrlController.java
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── orchestration/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/index.html
│   └── test/
│       └── java/com/schwab/agenticurl/
│           ├── controller/UrlControllerIntegrationTest.java
│           ├── orchestration/AgenticOrchestratorTest.java
│           └── service/ShortUrlServiceTest.java
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── README.md
└── .gitignore
```

---

## Prerequisites

### Recommended Software

- Java JDK 21
- Maven 3.9+
- Git (optional)
- Docker Desktop (optional)

Verify the Java and Maven installations:

```bash
java -version
mvn -version
```

Java should report version `21.x`.

### Windows Support

The application can run from PowerShell, Command Prompt, Git Bash, or IntelliJ IDEA.

---

## Installation and Startup

### Option A — Run with Maven

1. Extract the ZIP archive.
2. Open a terminal in the extracted project directory.
3. Run the tests:

```bash
mvn clean test
```

4. Start the application:

```bash
mvn spring-boot:run
```

The server starts at:

```text
http://localhost:8080
```

Open the demo UI:

```text
http://localhost:8080/
```

### Option B — Build and Run a JAR

```bash
mvn clean package
java -jar target/agentic-url-shortener-1.0.0.jar
```

### Option C — Run with Docker

From the project directory:

```bash
docker compose up --build
```

Open:

```text
http://localhost:8080/
```

Stop the environment:

```bash
docker compose down
```

---

## URL Shortener Demo

### Create a Short URL

**PowerShell:**

```powershell
curl.exe -X POST http://localhost:8080/api/urls `
  -H "Content-Type: application/json" `
  -d '{"url":"https://www.example.com/products/very/long/path","createdBy":"demo"}'
```

**Example response:**

```json
{
  "code": "Ab12XyZ",
  "shortUrl": "/r/Ab12XyZ",
  "originalUrl": "https://www.example.com/products/very/long/path",
  "createdAt": "2026-10-04T00:00:00Z"
}
```

> Short codes are randomly generated, so the actual value will differ.

### Redirect

Open the generated URL in a browser:

```text
http://localhost:8080/r/Ab12XyZ
```

The service responds with HTTP `302` and increments the click counter.

### Analytics

```http
GET /api/urls/Ab12XyZ/analytics
```

**Example response:**

```json
{
  "code": "Ab12XyZ",
  "originalUrl": "https://www.example.com/products/very/long/path",
  "clicks": 1,
  "createdAt": "2026-10-04T00:00:00Z"
}
```

---

## Agentic Orchestration API

### Create a Workflow

```http
POST /api/workflows
Content-Type: application/json
```

**Example request:**

```json
{
  "scenario": "GREENFIELD",
  "requirement": "Build a URL shortener with analytics and reliability features",
  "autoApprove": false
}
```

The response contains a `workflowId` and the initial workflow state.

### Supported Scenarios

```text
GREENFIELD
BROWNFIELD
AMBIGUOUS
```

### Why `autoApprove=false` Is Recommended

The assignment emphasizes controlled autonomy. Manual approval demonstrates that agents cannot automatically cross high-impact governance boundaries.

Approval gates occur before:

- `REQUIREMENTS` execution
- `IMPLEMENTATION` execution
- `RELEASE` completion

For a quick unattended demonstration, use `autoApprove=true`. This is a demo switch and **not** a production security model.

---

## Workflow Operations

### Get Workflow Status

```http
GET /api/workflows/{workflowId}
```

The workflow state includes:

- Workflow status and scenario
- Requirement
- Stage status and dependencies
- Attempts, outputs, and errors
- Approvals
- Retry and rollback counts
- Audit trail

### Approve a Human Gate

```http
POST /api/workflows/{workflowId}/approve
Content-Type: application/json
```

**Approve requirements:**

```json
{
  "stage": "REQUIREMENTS",
  "approved": true,
  "reason": "Reviewed normalized requirement and acceptance criteria"
}
```

**Approve implementation:**

```json
{
  "stage": "IMPLEMENTATION",
  "approved": true,
  "reason": "Implementation scope reviewed"
}
```

**Approve release:**

```json
{
  "stage": "RELEASE",
  "approved": true,
  "reason": "Release readiness reviewed"
}
```

### Safe Stop

```http
POST /api/workflows/{workflowId}/stop?reason=Operator%20requested%20stop
```

Safe-stop is terminal for the workflow. The stop event remains in the workflow audit trail.

### Dynamic Re-planning

```http
POST /api/workflows/{workflowId}/replan
Content-Type: application/json
```

**Example request:**

```json
{
  "requirement": "Add custom aliases and rate limiting while retaining analytics"
}
```

The orchestrator then:

1. Records the re-plan event.
2. Stores the changed requirement in workflow context.
3. Invalidates architecture and all downstream stages.
4. Returns the workflow to a human approval boundary.
5. Requires controlled continuation.

This demonstrates non-linear, stateful execution rather than a one-way task chain.

---

## Reliability Metrics

```http
GET /api/metrics/{workflowId}
```

The response includes:

- `stageSuccessRate`
- `retryCount`
- `rollbackCount`
- `endToEndLatencyMs`
- Workflow status

> These are prototype metrics. Production telemetry should additionally provide Prometheus/OpenTelemetry metrics, distributed traces, durable audit storage, and alerting.

---

## Assignment Scenarios

See [`docs/SCENARIOS.md`](docs/SCENARIOS.md) for complete scenario details.

### Greenfield

Starts with a new URL-shortener requirement and demonstrates complete decomposition and execution.

### Brownfield

Treats the shortener as an existing system and demonstrates impact analysis, backward compatibility, and enhancement-oriented planning.

### Ambiguous

Starts with an underspecified enterprise requirement, identifies ambiguity explicitly, uses only safe assumptions, and supports re-planning after stakeholder feedback.

---

## Testing Strategy

Run all tests:

```bash
mvn clean test
```

### Current Coverage

| Test Type | Coverage |
|---|---|
| Unit tests | URL validation, short-code generation, service behavior |
| Integration tests | Create URL, retrieve analytics, redirect, click-count increment |
| Orchestration tests | Explicit dependency graph, parallel branch dependencies, manual approval state |

### Recommended Production Coverage

- Concurrency and load testing
- Property-based short-code collision testing
- Database migration tests
- Security tests
- Rate-limit tests
- Contract tests
- Chaos and failure-injection tests
- Agent sandbox tests
- Policy-engine tests
- Audit immutability tests

---

## Engineering Design

### Requirement Understanding

The core problem is not merely “build a URL shortener.” It is:

> Build a working software artifact and demonstrate that an AI/agent system can move from an ambiguous human requirement to reviewable engineering outputs while remaining governed, observable, and reversible.

The design therefore separates:

- **Business service:** URL shortening, redirect, and analytics
- **Agent execution:** Requirement, design, implementation, test, documentation, and release stages
- **Governance:** Approvals, policy boundaries, retries, rollback, safe-stop, and audit lineage

### Key Engineering Decisions

#### H2 by Default

H2 eliminates database installation during the interview demo. JPA maintains a clean persistence boundary for a future PostgreSQL migration.

#### No External AI API Dependency

The prototype is deterministic and runnable offline. Bounded stage agents represent agent behavior; approved LLM/tool adapters can be added later behind the same boundaries.

#### Explicit DAG

A dependency graph makes ordering and parallelism visible. Testing and documentation do not depend on one another, but both depend on implementation.

#### Human Gates

Requirements, implementation, and release can materially affect engineering outcomes, so each has an approval boundary.

#### Bounded Autonomy

The prototype performs no arbitrary shell execution, source-code modification, or uncontrolled external side effect. This is deliberate risk control.

#### Re-planning

Upstream requirement changes invalidate dependent artifacts. Continuing with stale architecture would violate engineering correctness.

---

## Security and Compliance

### Prototype Guardrails

- Only HTTP/HTTPS destination URLs
- Centralized input validation
- No arbitrary agent command execution
- Explicit human approval boundaries
- Safe-stop endpoint
- Bounded retries
- Audit events for state-changing workflow actions
- Downstream invalidation after upstream changes

### Recommended Production Controls

- OAuth2/OIDC authentication
- RBAC/ABAC for approvals
- Secrets manager integration
- URL reputation checks and SSRF protection
- Rate limiting
- WAF/API gateway
- Immutable audit storage
- Encryption at rest and in transit
- PII/data-classification policy
- Signed artifacts
- Sandboxed agent execution
- Allowlisted tools and repositories
- Prompt-injection detection
- Code scanning: SAST, SCA, and DAST
- Change-ticket integration
- Four-eyes approval for releases

---

## Failure Handling

```text
Stage starts
   |
   +--> success --------------------> next eligible node
   |
   +--> failure
          |
          +--> attempts <= 2 -------> retry
          |
          +--> attempts > 2 --------> rollback event
                                      |
                                      v
                                  SAFE_STOP
```

The prototype fails closed: if orchestration cannot establish a safe state, it stops instead of continuing autonomously.

---

## Trade-offs and Limitations

| Area | Current Prototype | Production Direction |
|---|---|---|
| Workflow state | In-memory registry; lost on restart | PostgreSQL, Redis, or event log |
| Agents | Deterministic stage artifacts | Approved LLM/tool adapters |
| Analytics | Aggregate click count only | Event-level analytics, retention, privacy controls |
| Authentication | None | OAuth2/OIDC and authorization policy |
| Orchestration | Single-node, process-local | Durable distributed workflow engine, leases, idempotency, queue/event bus |

---

## Production Evolution

```text
API Gateway
    |
Workflow API ---> PostgreSQL workflow state
    |                    |
    v                    v
Policy Engine       Audit Event Store
    |
    v
Durable Workflow Engine / Queue
    |
    +--> Requirements Agent (sandbox)
    +--> Architecture Agent (sandbox)
    +--> Coding Agent (ephemeral workspace)
    +--> Test Agent (ephemeral workspace)
    +--> Documentation Agent
    +--> Release Agent
                 |
                 v
          Human Approval System
```

Recommended controls include idempotent activities, timeouts, leases, compensation handlers, artifact hashes, signed releases, OpenTelemetry traces, Prometheus metrics, and centralized SIEM/audit integration.

---

## Interview Demo Script

A strong 8–12 minute interview demonstration can follow this sequence:

1. Start the application with `mvn spring-boot:run`.
2. Open `http://localhost:8080/`.
3. Create a short URL.
4. Open the redirect and show the analytics click count.
5. Start a `GREENFIELD` workflow with `autoApprove=false`.
6. Show the initial human gate.
7. Approve `REQUIREMENTS`.
8. Show architecture and implementation gating.
9. Approve `IMPLEMENTATION`.
10. Show `TESTING` and `DOCUMENTATION` progressing independently.
11. Approve `RELEASE`.
12. Show the audit trail and metrics.
13. Start an `AMBIGUOUS` workflow and explain the identified ambiguities.
14. Trigger re-planning and show downstream invalidation.
15. Explain why unrestricted agent autonomy was intentionally avoided.

---

## Useful URLs

| Purpose | URL |
|---|---|
| Demo UI | `http://localhost:8080/` |
| H2 Console | `http://localhost:8080/h2-console` |
| Health endpoint | `http://localhost:8080/actuator/health` *(if Actuator is added)* |

### H2 Console Connection

```text
JDBC URL: jdbc:h2:file:./data/urlshortener
User:     sa
Password: <blank>
```

---

## Project Summary

### Plan and Rationale

Build the business capability first, then expose an explicit stateful SDLC graph around it. Keep agent permissions narrow, make high-impact boundaries human-controlled, and make failure behavior observable and reversible.

### Delivered Artifacts

- Working Spring Boot URL shortener
- REST API
- Browser demo UI
- H2 persistence
- Unit and integration tests
- Agentic DAG orchestrator
- Human approval gates
- Retry, rollback, safe-stop, and re-plan mechanisms
- Audit trail
- Reliability metrics
- Architecture document
- Scenario document
- CI workflow
- Docker support

### Primary Risks

- Agent hallucination
- Prompt injection
- Unauthorized changes
- Stale downstream artifacts
- Transient tool failures
- Data loss
- Approval bypass

### Validation

The prototype validates URL behavior through automated tests and orchestration behavior through dependency/gate tests and observable audit state.

### Assumptions

- Short codes are random seven-character identifiers
- HTTP/HTTPS URLs are sufficient for the demo
- H2 is acceptable for local development
- Workflow state does not need to survive a process restart in this prototype

### Limitations

The prototype does not include production authentication, distributed orchestration, durable workflow state, real LLM/tool integration, advanced analytics, or an enterprise policy engine.

---

## License and Interview Use

This project is supplied as an interview-assignment prototype. Add the organization’s preferred internal licensing and source-control policies before production use.

---

## Convenience Scripts

### Windows PowerShell

```powershell
.\run-local.ps1
```

### Windows Command Prompt

```bat
run-local.bat
```

Both scripts require Maven and JDK 21 to be installed.

---

## API Definition

A machine-readable OpenAPI 3 specification is available at:

```text
openapi.yaml
```

Import it into Swagger UI, Postman, or another API client.
