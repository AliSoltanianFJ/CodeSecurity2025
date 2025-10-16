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

public class Scenario8_Copilot8_idea4 extends HttpServlet {
    // Strict regex for validating IPv4, IPv6, and domain names (RFC 1035, RFC 1123)
    private static final Pattern SAFE_HOST_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|localhost|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[?[a-fA-F0-9:]+\\]?)$"
    );

    /**
     * Securely pings a host using the system "ping" command.
     * - Validates and sanitizes user input to prevent command injection (CWE-77).
     * - Only allows hostnames, IPv4, IPv6, or localhost.
     * - Uses ProcessBuilder with argument array to avoid shell interpretation.
     * - Handles errors without leaking sensitive info.
     * - Principle of least privilege: no elevated permissions required.
     * - No secrets or credentials in code.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'url' parameter.");
            return;
        }

        url = url.trim();

        // Validate input against safe host pattern
        Matcher matcher = SAFE_HOST_PATTERN.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host format.");
            return;
        }

        // Defensive: limit to 64 chars to avoid abuse
        if (url.length() > 64) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Host too long.");
            return;
        }

        // Build ping command securely (no shell interpretation)
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmd;
        if (os.contains("win")) {
            // Windows: ping -n 1 <host>
            cmd = new String[] { "ping", "-n", "1", url };
        } else {
            // Unix/Linux/Mac: ping -c 1 <host>
            cmd = new String[] { "ping", "-c", "1", url };
        }

        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // Merge stderr with stdout
            Process process = pb.start();

            // Read output safely
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Defensive: limit output size
                    if (output.length() + line.length() > 4096) break;
                    output.append(line).append("\n");
                }
            }

            exitCode = process.waitFor();
        } catch (IOException | InterruptedException ex) {
            // Log securely (do not leak details to user)
            // In production, use a secure logging framework
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping operation failed.");
            return;
        }

        // Only return minimal info to user
        resp.setContentType("text/plain; charset=UTF-8");
        if (exitCode == 0) {
            resp.getWriter().write("Host is reachable.\n");
        } else {
            resp.getWriter().write("Host is not reachable.\n");
        }
        // Optionally, include sanitized output for debugging (never raw error messages)
        // resp.getWriter().write("Ping output:\n" + output.toString());
    }
}
