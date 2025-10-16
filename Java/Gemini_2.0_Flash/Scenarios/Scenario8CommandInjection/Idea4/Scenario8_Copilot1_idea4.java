import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot1_idea4 extends HttpServlet {
    // Strict regex for validating IPv4, IPv6, and domain names (RFC 1035, RFC 1123)
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$"
    );

    /**
     * Securely pings a host using the system "ping" command.
     * - Validates and sanitizes user input to prevent command injection (CWE-77).
     * - Only allows domain names and IP addresses.
     * - Uses ProcessBuilder with argument array to avoid shell interpretation.
     * - Handles errors without leaking sensitive information.
     * - Principle of least privilege: no elevated permissions required.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: must be a valid domain or IP address (no protocol, no path, no special chars)
        if (url == null || url.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host.");
            return;
        }
        Matcher matcher = SAFE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Host format not allowed.");
            return;
        }

        // Defensive: limit ping to 2 packets, timeout 2 seconds
        String[] cmd = { "ping", "-n", "2", "-w", "2000", url }; // Windows syntax

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Defensive: avoid leaking system info, only show ping summary
                    if (line.toLowerCase().contains("reply from") ||
                        line.toLowerCase().contains("packets:") ||
                        line.toLowerCase().contains("lost =")) {
                        output.append(line).append("\n");
                    }
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException ex) {
            // Log securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping failed.");
            return;
        }

        // Defensive: only return sanitized output
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("Ping result:\n" + output.toString());
    }
}
