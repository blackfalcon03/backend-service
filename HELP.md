# Order Service & AI AutoTriage Platform (SDET Solution)

An enterprise-grade Java 17 / Spring Boot backend service integrated with **OpenAPI 3.0** documentation and an **AI-powered AutoTriage Agent** built on **LangChain4j**. 

This repository demonstrates how Senior SDETs can automate Root Cause Analysis (RCA) on failed automated tests—both locally in IntelliJ before submitting a pull request and automatically inside GitHub Actions CI/CD.

---

## 🚀 Key Features

1. **Order Management REST API:**
   - Full CRUD and lifecycle operations (`POST /api/v1/orders`, `GET /api/v1/orders/{id}`, `GET /api/v1/orders`, `POST /api/v1/orders/{id}/cancel`).
   - Dynamic coupon discount calculations (`SAVE10` = 10% off, `SAVE20` = 20% off).
   - Jakarta Validation with RFC 7807 `ProblemDetail` responses.
   - Live **OpenAPI 3.0 (Swagger UI)** specification at `/swagger-ui.html`.

2. **Automated Testing Suite (JUnit 5 + MockMvc):**
   - 10+ comprehensive test scenarios covering positive flows, boundary checks, and negative validation failures.
   - Automated OpenAPI specification verification (`OpenApiDocumentationTest`).
   - Structured SLF4J execution logs mapped into JUnit XML reports.

3. **AI AutoTriage Agent (LangChain4j):**
   - Multi-provider architecture with zero-code switching between **Google Gemini**, **OpenAI**, **self-hosted Ollama (VPN/Privacy)**, and an **offline Mock mode**.
   - Ingests failed test names, exception messages, stack traces, application trace logs, and `git diff` changes.
   - Classifies failures into *Product Bug (Regression)*, *Flaky Test*, *Contract Drift*, or *Environment Issue* with confidence scores and recommended code fixes.

4. **Dual-Mode Execution:**
   - **Local Shift-Left:** SDETs can run triage locally via `./gradlew triage` before committing code.
   - **CI/CD Integration:** Automatically triggers in GitHub Actions on test failure (`if: failure()`) and publishes formatted Markdown reports to the GitHub Actions Job Summary.

---

## 🛠️ Tech Stack & Requirements

| Component | Technology |
|---|---|
| **Runtime & Language** | Java 17 |
| **Framework** | Spring Boot 4.x / Spring 7 |
| **Build Tool** | Gradle 9.x |
| **API Documentation** | `springdoc-openapi-starter-webmvc-ui` (OpenAPI 3.0) |
| **Testing** | JUnit 5, AssertJ, MockMvc |
| **AI Orchestration** | LangChain4j (`0.36.2`) |
| **LLM Support** | Google Gemini (Default), OpenAI, Self-Hosted Ollama, Mock |
| **CI/CD** | GitHub Actions |

---

## ⚙️ Environment Configuration (`.env`)

Local environment variables are managed using a `.env` file in the project root. Copy the template to start:

```bash
cp .env.example .env
```

### Supported Settings:

```properties
# LLM Provider selection: gemini | openai | ollama | mock
LLM_PROVIDER=gemini

# Google Gemini Configuration (Default)
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-1.5-flash

# OpenAI Configuration (Optional)
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o-mini

# Self-Hosted Ollama Configuration (Optional / Local VPN)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2

# Demo Failure Simulation Toggle (true | false)
SIMULATE_BUG=false
```

> [!NOTE]
> `.env` is automatically git-ignored to prevent credential leaks. If no API key is configured, set `LLM_PROVIDER=mock` to run offline demos without consuming tokens.

---

## 🏃 Running the Application Locally

Start the Spring Boot service:
```bash
./gradlew bootRun
```
*Note: Docker Compose is disabled by default (`spring.docker.compose.enabled: false`) so the service runs standalone without requiring Docker.*

Once started, access:
- **Interactive Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3.0 JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 🧪 Running Automated Tests

Run the full test suite with Gradle:
```bash
./gradlew test
```

To force-rerun all tests bypassing Gradle cache:
```bash
./gradlew test --rerun-tasks
```

Test reports are generated at:
- **HTML Report:** `build/reports/tests/test/index.html`
- **JUnit XML:** `build/test-results/test/`

---

## 🤖 Running the AI AutoTriage Agent

If tests pass, the triage agent verifies health:
```bash
./gradlew triage
# Output: ✅ All tests passed! No triage required.
```

### Demonstrating a Failure Scenario in Real Time

To demonstrate the AutoTriage Agent during a presentation without editing code:

1. **Trigger a failure using the built-in feature toggle:**
   ```bash
   SIMULATE_BUG=true ./gradlew test --rerun-tasks
   ```
   *(This intentionally alters the discount logic from 10% to 15%, causing `OrderControllerTest` to fail).*

2. **Run the AI AutoTriage Agent:**
   ```bash
   ./gradlew triage
   ```

3. **Sample Generated Report:**
   ```markdown
   =================================================
           AI AutoTriage Agent (SDET Platform)      
   =================================================
   ⚠️  Detected 1 failed test(s). Gathering context...
   🤖 Querying AI AutoTriage Model via LangChain4j...

   -------------------------------------------------
   AI TRIAGE REPORT
   -------------------------------------------------
   ### 1. Classification & Confidence
   - **Category**: Product Bug (Regression)
   - **Confidence Score**: 95%
   - **Severity**: High

   ### 2. Root Cause Summary
   The test 'shouldCreateOrderSuccessfully' failed with an AssertionError.
   The discount calculation returned 42.50 instead of expected 45.00 due to a 15% discount
   being applied instead of 10%.

   ### 3. Culprit Location
   File: `net.ey.backendservice.service.OrderService.java`
   Method: `applyDiscount(BigDecimal subtotal, String couponCode)`

   ### 4. Recommended Fix & Action
   Update line in OrderService to restore 0.90 multiplier:
   ```java
   case "SAVE10" -> subtotal.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);
   ```
   -------------------------------------------------
   📄 Saved triage report to: build/reports/ai-triage-report.md
   ```

4. **Return to clean state:**
   ```bash
   ./gradlew test --rerun-tasks
   ```

---

## ☁️ GitHub Actions CI/CD Pipeline

The workflow defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) executes on every push and pull request to `main`.

### Repository Configuration in GitHub:
- **Secrets (`Settings -> Secrets and variables -> Actions -> Secrets`):**
  - `GEMINI_API_KEY`: Your Google Gemini API key.
  - `OPENAI_API_KEY`: *(Optional)* If using OpenAI.
- **Variables (`Settings -> Secrets and variables -> Actions -> Variables`):**
  - `LLM_PROVIDER`: `gemini` (default), `openai`, or `mock`.
  - `GEMINI_MODEL`: `gemini-1.5-flash` (default) or `gemini-1.5-pro`.

### One-Click Cloud Demo (`workflow_dispatch`):
You can trigger the entire demo in GitHub Actions from your browser:
1. Go to the **Actions** tab in your GitHub repository.
2. Select **"SDET CI & AI AutoTriage Pipeline"**.
3. Click **"Run workflow"** and check **"Simulate a business logic regression bug"**.
4. Click **Run**. The run will fail as expected, execute the AutoTriage Agent, and render the complete Markdown triage report directly on the **Job Summary** page!
