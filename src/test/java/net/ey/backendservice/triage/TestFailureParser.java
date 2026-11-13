package net.ey.backendservice.triage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestFailureParser {

    public record TestFailure(
            String className,
            String testName,
            String failureMessage,
            String stackTrace,
            String executionLogs
    ) {}

    public static List<TestFailure> parseFailures(Path testResultsDir) {
        List<TestFailure> failures = new ArrayList<>();
        if (!Files.exists(testResultsDir)) {
            return failures;
        }

        try (Stream<Path> paths = Files.walk(testResultsDir)) {
            List<File> xmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("TEST-") && p.getFileName().toString().endsWith(".xml"))
                    .map(Path::toFile)
                    .toList();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (File xmlFile : xmlFiles) {
                try {
                    Document doc = builder.parse(xmlFile);
                    NodeList testCases = doc.getElementsByTagName("testcase");

                    // Suite-level system-out if present
                    String suiteSystemOut = "";
                    NodeList suiteOutNodes = doc.getElementsByTagName("system-out");
                    if (suiteOutNodes.getLength() > 0) {
                        suiteSystemOut = suiteOutNodes.item(0).getTextContent().trim();
                    }

                    for (int i = 0; i < testCases.getLength(); i++) {
                        Element testCase = (Element) testCases.item(i);
                        NodeList failureNodes = testCase.getElementsByTagName("failure");

                        if (failureNodes.getLength() > 0) {
                            Element failureEl = (Element) failureNodes.item(0);
                            String className = testCase.getAttribute("classname");
                            String testName = testCase.getAttribute("name");
                            String message = failureEl.getAttribute("message");
                            String stackTrace = failureEl.getTextContent().trim();

                            // Case-level system-out or fallback to suite-level
                            String executionLogs = "";
                            NodeList caseOutNodes = testCase.getElementsByTagName("system-out");
                            if (caseOutNodes.getLength() > 0) {
                                executionLogs = caseOutNodes.item(0).getTextContent().trim();
                            } else if (!suiteSystemOut.isBlank()) {
                                executionLogs = suiteSystemOut;
                            }

                            // Truncate logs if too lengthy (keep last 30 lines where error happens)
                            String[] logLines = executionLogs.split("\n");
                            if (logLines.length > 30) {
                                StringBuilder sb = new StringBuilder();
                                for (int j = Math.max(0, logLines.length - 30); j < logLines.length; j++) {
                                    sb.append(logLines[j]).append("\n");
                                }
                                executionLogs = "... [earlier logs truncated]\n" + sb.toString().trim();
                            }

                            // Shorten very long stack traces to top 25 lines
                            String[] lines = stackTrace.split("\n");
                            if (lines.length > 25) {
                                StringBuilder sb = new StringBuilder();
                                for (int j = 0; j < 25; j++) {
                                    sb.append(lines[j]).append("\n");
                                }
                                sb.append("... [truncated for brevity]");
                                stackTrace = sb.toString();
                            }

                            failures.add(new TestFailure(className, testName, message, stackTrace, executionLogs));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Failed to parse test XML: " + xmlFile.getName() + " -> " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading test results directory: " + e.getMessage());
        }

        return failures;
    }
}
