package net.ey.backendservice.triage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Main AI AutoTriage Agent for SDETs.
 * Runs both locally before PR submission and inside GitHub Actions CI.
 */
public class AutoTriageAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Principal Software Development Engineer in Test (SDET) and QA Architect.
            Your job is to perform deep Root Cause Analysis (RCA) on failed automated tests.
            
            Given:
            1. The failed test method(s), error message, stack trace, and application execution logs.
            2. The Git diff (recent changes in the codebase or pull request).
            3. The Git status and latest commit.
            
            Provide a clear, professional SDET Triage Report formatted in Markdown:
            
            ### 1. Classification & Confidence
            - **Category**: [Product Bug (Regression) | Flaky / Timing | Contract / Schema Drift | Broken Test Logic | Environment Issue]
            - **Confidence Score**: [0-100%]
            - **Severity**: [Blocker | High | Medium | Low]
            
            ### 2. Root Cause Summary
            - Briefly explain WHY the test failed in 2-3 clear sentences.
            - Explicitly link how the code change in the Git diff (or application execution logs) caused the failure.
            
            ### 3. Culprit Location
            - File and method/line responsible for the issue.
            
            ### 4. Recommended Fix & Action
            - Provide the exact corrected code snippet (diff or replacement).
            - State whether the product code or the test code needs updating.
            
            Be objective, precise, and avoid speculative fluff.
            """;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("        AI AutoTriage Agent (SDET Platform)      ");
        System.out.println("=================================================");

        Path testResultsDir = Path.of("build", "test-results", "test");
        List<TestFailureParser.TestFailure> failures = TestFailureParser.parseFailures(testResultsDir);

        if (failures.isEmpty()) {
            System.out.println("✅ All tests passed! No triage required.");
            return;
        }

        System.out.printf("⚠️  Detected %d failed test(s). Gathering context...%n", failures.size());

        GitContextCollector.GitContext gitContext = GitContextCollector.collect();

        // Build User Prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("## FAILED TESTS:\n");
        for (int i = 0; i < failures.size(); i++) {
            TestFailureParser.TestFailure f = failures.get(i);
            promptBuilder.append(String.format("### Test #%d: %s#%s%n", i + 1, f.className(), f.testName()));
            promptBuilder.append(String.format("- **Message**: %s%n", f.failureMessage()));
            promptBuilder.append("- **Stack Trace Snippet**:\n```\n").append(f.stackTrace()).append("\n```\n");
            if (f.executionLogs() != null && !f.executionLogs().isBlank()) {
                promptBuilder.append("- **Application Execution Logs**:\n```log\n").append(f.executionLogs()).append("\n```\n");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("## GIT CONTEXT:\n");
        promptBuilder.append("- **Latest Commit**: ").append(gitContext.lastCommit()).append("\n");
        promptBuilder.append("- **Modified Files**:\n```\n").append(gitContext.status()).append("\n```\n");
        promptBuilder.append("- **Git Diff**:\n```diff\n").append(gitContext.diff().isBlank() ? "(No diff detected)" : gitContext.diff()).append("\n```\n");

        System.out.println("🤖 Querying AI AutoTriage Model via LangChain4j...");
        long startTime = System.currentTimeMillis();

        try {
            ChatLanguageModel model = LlmModelFactory.createModel();
            Response<AiMessage> response = model.generate(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(promptBuilder.toString())
            );

            long elapsed = System.currentTimeMillis() - startTime;
            String triageReport = response.content().text();

            System.out.println("\n-------------------------------------------------");
            System.out.printf("AI TRIAGE REPORT (Generated in %d ms)%n", elapsed);
            System.out.println("-------------------------------------------------");
            System.out.println(triageReport);
            System.out.println("-------------------------------------------------\n");

            // Save report to file
            saveReportToFile(triageReport);

            // Export to GitHub Actions Step Summary if running in CI
            publishToGitHubActionsIfPresent(triageReport);

        } catch (Exception e) {
            System.err.println("❌ AutoTriage execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveReportToFile(String report) {
        try {
            Path reportsDir = Path.of("build", "reports");
            Files.createDirectories(reportsDir);
            Path reportPath = reportsDir.resolve("ai-triage-report.md");
            Files.writeString(reportPath, report);
            System.out.println("📄 Saved triage report to: " + reportPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write triage report file: " + e.getMessage());
        }
    }

    private static void publishToGitHubActionsIfPresent(String report) {
        String stepSummaryFile = System.getenv("GITHUB_STEP_SUMMARY");
        if (stepSummaryFile != null && !stepSummaryFile.isBlank()) {
            try {
                Path summaryPath = Path.of(stepSummaryFile);
                String header = "## 🤖 SDET AI AutoTriage Report\n\n";
                Files.writeString(summaryPath, header + report + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.out.println("🚀 Published triage report to GitHub Actions Job Summary.");
            } catch (IOException e) {
                System.err.println("Failed to write to GITHUB_STEP_SUMMARY: " + e.getMessage());
            }
        }
    }
}
