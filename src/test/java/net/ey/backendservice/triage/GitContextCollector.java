package net.ey.backendservice.triage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class GitContextCollector {

    public record GitContext(
            String status,
            String diff,
            String lastCommit
    ) {}

    public static GitContext collect() {
        String status = runCommand("git", "status", "-s");
        String lastCommit = runCommand("git", "log", "-1", "--oneline");

        // First check for uncommitted changes (local run)
        String diff = runCommand("git", "diff", "HEAD");
        if (diff.isBlank()) {
            // If working tree is clean (e.g. CI PR build or post-commit), check last commit diff
            diff = runCommand("git", "diff", "HEAD~1");
        }

        // Limit diff length to 200 lines to avoid token overflows
        String[] diffLines = diff.split("\n");
        if (diffLines.length > 200) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                sb.append(diffLines[i]).append("\n");
            }
            sb.append("\n... [diff truncated to 200 lines]");
            diff = sb.toString();
        }

        return new GitContext(status, diff, lastCommit);
    }

    private static String runCommand(String... args) {
        try {
            Process process = new ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }
            return output.toString().trim();
        } catch (Exception e) {
            return "[Unable to retrieve git context: " + e.getMessage() + "]";
        }
    }
}
