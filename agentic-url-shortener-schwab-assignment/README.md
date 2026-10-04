# Agentic Software Engineering System — URL Shortener

> **Interview Assignment:** Build an Agentic Software Engineering System — URL Shortener
>
> **Classification:** Schwab Internal
>
> **Prototype goal:** Demonstrate controlled agentic execution from requirement understanding through architecture, implementation, testing, documentation and release readiness, while also providing a working URL shortener service.

## 1. What this application does

This repository contains two related capabilities:

1. **A working URL shortener service**
   - Create a short URL from an HTTP/HTTPS URL.
   - Resolve a short URL through a redirect endpoint.
   - Track click count.
   - Query analytics.
   - Validate input and return consistent errors.

2. **A governed agentic SDLC orchestration prototype**
   - Accept a high-level engineering requirement.
   - Normalize/decompose it into explicit lifecycle stages.
   - Execute a dependency graph instead of a simple linear chain.
   - Run testing and documentation paths in parallel.
   - Synchronize at the release gate.
   - Require human approval for high-impact stages.
   - Enforce bounded retries.
   - Record rollback and safe-stop events.
   - Support dynamic re-planning when an upstream requirement changes.
   - Preserve audit/decision lineage and reliability metrics.

The agent stages in this prototype are intentionally bounded simulators. They generate reviewable engineering artifacts and decisions instead of executing arbitrary code or shell commands. This demonstrates the governance architecture expected for a production agentic system without creating an unsafe unrestricted coding agent.

---

## 2. Architecture at a glance

```text
                    +----------------------+
                    |   Browser Demo UI     |
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

Detailed architecture is in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## 3. Technology stack

| Area | Technology | Why |
|---|---|---|
| Language | Java 21 | Modern LTS Java, virtual threads and strong ecosystem |
| Framework | Spring Boot 3.5.x | Production-oriented REST/application framework |
| API | Spring Web | REST endpoints and redirect handling |
| Validation | Spring Validation / Jakarta Validation | Boundary validation |
| Persistence | Spring Data JPA | Clean repository abstraction; easy H2/PostgreSQL transition |
| Default DB | H2 file database | Zero external setup for interview demo |
| Tests | JUnit 5 + Mockito + MockMvc | Unit and API integration coverage |
| Build | Maven | Standard Java build lifecycle |
| UI | HTML/CSS/JavaScript | No Node/npm dependency for the demo UI |
| Container | Docker | Repeatable local execution |
| CI | GitHub Actions | Build/test automation example |
| Orchestration | Custom stateful DAG in Java | Explicit dependencies, gates, parallel paths and governance |
| Concurrency | Java virtual threads + ExecutorService | Simple bounded parallel stage execution |
| API style | JSON REST | Easy to demo with browser/curl/Postman |

---

## 4. Project structure

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

## 5. Prerequisites for local machine

### Recommended

- Java JDK 21
- Maven 3.9+
- Git (optional)
- Docker Desktop (optional)

Check:

```bash
java -version
mvn -version
```

You should see Java 21.x.

### Windows

The application works from PowerShell, Command Prompt, Git Bash or IntelliJ IDEA.

---

## 6. Installation

### Option A — Maven directly

1. Extract the ZIP.
2. Open a terminal in the extracted project folder.
3. Run:

```bash
mvn clean test
```

4. Start the application:

```bash
mvn spring-boot:run
```

The server starts on:

```text
http://localhost:8080
```

Open the demo UI:

```text
http://localhost:8080/
```

### Option B — Build a JAR

```bash
mvn clean package
java -jar target/agentic-url-shortener-1.0.0.jar
```

### Option C — Docker

From the project directory:

```bash
docker compose up --build
```

Then open:

```text
http://localhost:8080/
```

To stop:

```bash
docker compose down
```

---

## 7. First URL-shortener demo

### Create a short URL

PowerShell:

```powershell
curl.exe -X POST http://localhost:8080/api/urls `
  -H "Content-Type: application/json" `
  -d '{"url":"https://www.example.com/products/very/long/path","createdBy":"demo"}'
```

Example response:

```json
{
  "code": "Ab12XyZ",
  "shortUrl": "/r/Ab12XyZ",
  "originalUrl": "https://www.example.com/products/very/long/path",
  "createdAt": "2026-10-04T00:00:00Z"
}
```

The code is randomly generated, so your value will differ.

### Redirect

Open:

```text
http://localhost:8080/r/Ab12XyZ
```

The service returns HTTP 302 and increments the click counter.

### Analytics

```text
GET http://localhost:8080/api/urls/Ab12XyZ/analytics
```

Example:

```json
{
  "code": "Ab12XyZ",
  "originalUrl": "https://www.example.com/products/very/long/path",
  "clicks": 1,
  "createdAt": "2026-10-04T00:00:00Z"
}
```

---

## 8. Agentic orchestration API

### Create a workflow

```http
POST /api/workflows
Content-Type: application/json
```

Example:

```json
{
  "scenario": "GREENFIELD",
  "requirement": "Build a URL shortener with analytics and reliability features",
  "autoApprove": false
}
```

Response contains a `workflowId` and the initial state.

### Scenario values

```text
GREENFIELD
BROWNFIELD
AMBIGUOUS
```

### Why `autoApprove=false` is the recommended demo

The assignment emphasizes controlled autonomy. Manual approval demonstrates that agents do not automatically cross high-impact governance boundaries.

Approval gates are placed before:

- REQUIREMENTS execution
- IMPLEMENTATION execution
- RELEASE completion

For a quick unattended demo, set `autoApprove=true`. This is a demonstration switch, not a production security model.

---

## 9. Check workflow status

```http
GET /api/workflows/{workflowId}
```

The state contains:

- workflow status
- scenario
- requirement
- stage status
- dependencies
- attempts
- outputs
- errors
- approvals
- retry count
- rollback count
- audit trail

---

## 10. Approve a human gate

```http
POST /api/workflows/{workflowId}/approve
Content-Type: application/json
```

Example:

```json
{
  "stage": "REQUIREMENTS",
  "approved": true,
  "reason": "Reviewed normalized requirement and acceptance criteria"
}
```

Later approve:

```json
{
  "stage": "IMPLEMENTATION",
  "approved": true,
  "reason": "Implementation scope reviewed"
}
```

And release:

```json
{
  "stage": "RELEASE",
  "approved": true,
  "reason": "Release readiness reviewed"
}
```

---

## 11. Safe stop

```http
POST /api/workflows/{workflowId}/stop?reason=Operator%20requested%20stop
```

Safe-stop is terminal for that workflow. The event is retained in its audit trail.

---

## 12. Dynamic re-planning

If the stakeholder changes the requirement:

```http
POST /api/workflows/{workflowId}/replan
Content-Type: application/json
```

Example:

```json
{
  "requirement": "Add custom aliases and rate limiting while retaining analytics"
}
```

The orchestrator:

1. Records the re-plan event.
2. Stores the changed requirement in workflow context.
3. Invalidates architecture and all downstream stages.
4. Moves the workflow back to a human approval boundary.
5. Requires controlled continuation.

This demonstrates non-linear/stateful execution rather than a one-way task chain.

---

## 13. Reliability metrics

```http
GET /api/metrics/{workflowId}
```

Returns:

- `stageSuccessRate`
- `retryCount`
- `rollbackCount`
- `endToEndLatencyMs`
- workflow status

These are prototype metrics. Production telemetry should additionally expose Prometheus/OpenTelemetry metrics, distributed traces, durable audit storage and alerting.

---

## 14. Three required assignment scenarios

See [`docs/SCENARIOS.md`](docs/SCENARIOS.md).

### Greenfield

Starts with a new URL shortener requirement and demonstrates complete decomposition and execution.

### Brownfield

Treats the shortener as an existing system and demonstrates impact analysis, backward compatibility and enhancement-oriented planning.

### Ambiguous

Starts with an underspecified enterprise requirement, explicitly identifies ambiguity, chooses only safe assumptions and supports re-planning after stakeholder feedback.

---

## 15. Testing strategy

Run all tests:

```bash
mvn clean test
```

Current test coverage demonstrates:

### Unit tests

- URL validation
- short-code generation
- service behavior

### Integration tests

- create short URL
- retrieve analytics
- redirect
- click-count increment

### Orchestration tests

- explicit dependency graph
- parallel branch dependencies
- manual approval state

For production, expand with:

- concurrency/load testing
- property-based short-code collision tests
- database migration tests
- security tests
- rate-limit tests
- contract tests
- chaos/failure injection
- agent sandbox tests
- policy-engine tests
- audit immutability tests

---

## 16. Requirement understanding

The assignment's core engineering problem is not merely "make a URL shortener". It is:

> Build a working software artifact and demonstrate that an AI/agent system can move from an ambiguous human requirement to reviewable engineering outputs while remaining governed, observable and reversible.

Therefore the design separates:

- **Business service:** URL shortening/redirect/analytics.
- **Agent execution:** requirement/design/implementation/test/docs/release stages.
- **Governance:** approvals, policy boundaries, retries, rollback, safe stop and audit lineage.

---

## 17. Key engineering decisions

### H2 by default

H2 eliminates database installation during the interview demo. JPA keeps the persistence boundary clean enough to migrate to PostgreSQL.

### No external AI API dependency

The prototype must be deterministic and runnable offline. Agent behavior is represented by bounded stage agents. A production implementation could plug in approved LLM/tool adapters behind these boundaries.

### Explicit DAG

A dependency graph makes sequencing and parallelism visible. Testing and documentation do not depend on each other, but both depend on implementation.

### Human gates

Requirements, implementation and release can materially affect engineering outcomes. They therefore have approval boundaries.

### Bounded autonomy

No arbitrary shell execution, source-code modification or external side effects are performed by the prototype agent. This is deliberate risk control.

### Re-planning

Upstream requirement changes invalidate dependent artifacts. Continuing with stale architecture would violate engineering correctness.

---

## 18. Security and compliance guardrails

Prototype guardrails include:

- only HTTP/HTTPS destination URLs
- centralized input validation
- no arbitrary command execution by agents
- explicit human approval boundaries
- safe-stop endpoint
- bounded retries
- audit events for state-changing workflow actions
- downstream invalidation after upstream change

Production additions should include:

- OAuth2/OIDC authentication
- RBAC/ABAC for approvals
- secrets manager integration
- URL reputation/SSRF protection
- rate limiting
- WAF/API gateway
- immutable audit storage
- encryption at rest/in transit
- PII/data-classification policy
- signed artifacts
- sandboxed agent execution
- allowlisted tools and repositories
- prompt-injection detection
- code scanning/SAST/SCA/DAST
- change-ticket integration
- four-eyes approval for release

---

## 19. Failure handling model

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

The prototype intentionally fails closed: if orchestration cannot establish a safe state, it stops rather than continuing autonomously.

---

## 20. Trade-offs and limitations

### In-memory workflow registry

Workflow state is currently stored in memory. Restarting the application loses orchestration state. A production system should use durable storage such as PostgreSQL/Redis/event log.

### Simulated agents

The stage outputs are deterministic artifacts, not actual LLM-generated code changes. This keeps the demo safe and reproducible. The architecture is designed so real agent adapters can be introduced later.

### Basic analytics

Analytics currently stores only total click count. A production system may need timestamped events, unique visitors, referrers, device data and retention policies.

### No authentication

The demo intentionally has no authentication to keep setup simple. Production APIs must be protected.

### Single-node execution

The orchestrator is process-local. A production design should use durable workflow state, distributed locks/leases, idempotency keys and a queue/event bus.

---

## 21. Production evolution path

A production-grade version can evolve toward:

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

Recommended controls include idempotent activities, timeouts, leases, compensation handlers, artifact hashes, signed releases, OpenTelemetry traces, Prometheus metrics and centralized SIEM/audit integration.

---

## 22. Interview demonstration script

A strong 8–12 minute demo can follow this order:

1. Start application with `mvn spring-boot:run`.
2. Open `http://localhost:8080/`.
3. Create a short URL.
4. Open the redirect and show analytics click count.
5. Start a GREENFIELD workflow with `autoApprove=false`.
6. Show the initial human gate.
7. Approve REQUIREMENTS.
8. Show architecture and implementation gating.
9. Approve IMPLEMENTATION.
10. Show TESTING and DOCUMENTATION progressing independently.
11. Approve RELEASE.
12. Show audit trail and metrics.
13. Start an AMBIGUOUS workflow and explain identified ambiguity.
14. Trigger re-plan and show downstream invalidation.
15. Explain why unrestricted agent autonomy was intentionally avoided.

---

## 23. Useful URLs

Once running:

- Demo UI: `http://localhost:8080/`
- H2 console: `http://localhost:8080/h2-console`
- Health: `http://localhost:8080/actuator/health` (if actuator is added; not enabled in this minimal dependency set)

H2 console JDBC URL:

```text
jdbc:h2:file:./data/urlshortener
```

User:

```text
sa
```

Password: blank.

---

## 24. Final engineering summary

### Plan / rationale

Build the business capability first, then expose an explicit stateful SDLC graph around it. Keep agent permissions narrow, make high-impact boundaries human-controlled, and make failure behavior observable and reversible.

### Artifacts

- Working Spring Boot URL shortener
- REST API
- Browser demo UI
- H2 persistence
- Unit/integration tests
- Agentic DAG orchestrator
- Human approval gates
- Retry/rollback/safe-stop/re-plan mechanisms
- Audit trail
- Reliability metrics
- Architecture document
- Scenario document
- CI workflow
- Docker support

### Risks

Agent hallucination, prompt injection, unauthorized changes, stale downstream artifacts, transient tool failures, data loss and approval bypass are major production risks.

### Validation

The prototype validates URL behavior through automated tests and validates orchestration behavior through dependency/gate tests and observable audit state.

### Assumptions

- Short codes are random seven-character identifiers.
- HTTP/HTTPS URLs are sufficient for the demo.
- H2 is acceptable for local development.
- Workflow state need not survive a process restart in this prototype.

### Limitations

No production authentication, distributed orchestration, durable workflow store, real LLM/tool integration, advanced analytics or enterprise policy engine is included.

---

## 25. License / interview use

This project is supplied as an interview-assignment prototype. Add the organization's preferred internal licensing and source-control policies before production use.

## 26. Convenience run scripts

Windows PowerShell:

```powershell
.\run-local.ps1
```

Windows Command Prompt:

```bat
run-local.bat
```

The scripts expect Maven and JDK 21 to be installed.

---

## 27. API definition

A machine-readable OpenAPI 3 specification is included at:

```text
openapi.yaml
```

You can import it into Swagger UI, Postman or another API tool.

