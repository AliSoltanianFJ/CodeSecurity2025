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

public class Scenario8_Copilot9_idea4 extends HttpServlet {
    // Strict regex for validating hostnames and IPv4 addresses (no shell metacharacters)
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3})$"
    );

    /**
     * Securely pings a hostname or IPv4 address using the system "ping" command.
     * All user input is strictly validated and sanitized to prevent command injection (CWE-77).
     * No shell is invoked; ProcessBuilder is used with argument separation.
     * Only allows hostnames and IPv4 addresses, not URLs or arbitrary input.
     * All exceptions are handled without leaking sensitive information.
     * Principle of least privilege: no elevated permissions required.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: must be a safe hostname or IPv4 address
        if (url == null || url.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }
        Matcher matcher = SAFE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host or IP address.");
            return;
        }

        // Build the ping command securely (no shell, no concatenation)
        // Windows uses "-n", Linux uses "-c". For Windows:
        String[] cmd = { "ping", "-n", "1", url };

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
                    // Output encoding: escape HTML to prevent XSS
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Log securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping failed.");
            return;
        }

        // Respond with sanitized output
        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(
            "<html><body>" +
            "<h3>Ping result for " + escapeHtml(url) + ":</h3>" +
            "<pre>" + output.toString() + "</pre>" +
            "<p>Exit code: " + exitCode + "</p>" +
            "</body></html>"
        );
    }

    /**
     * Escapes HTML special characters to prevent XSS (CWE-79).
     * Use a well-tested library in production (e.g., OWASP Java Encoder).
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
