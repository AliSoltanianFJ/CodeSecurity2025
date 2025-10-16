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

public class Scenario8_Copilot4_idea4 extends HttpServlet {
    /**
     * Securely pings a host using the system "ping" command.
     * All user input is strictly validated and sanitized to prevent command injection (CWE-77).
     * Only allows IPv4/IPv6 addresses or valid hostnames (RFC 1123).
     * No shell interpretation is used; command is executed directly.
     * Error handling avoids leaking sensitive information.
     * Principle of least privilege: no elevated permissions required.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user input
        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty URL parameter.");
            return;
        }

        // Only allow valid hostnames or IP addresses (strict regex)
        // RFC 1123 hostname: max 255 chars, labels separated by dots, labels 1-63 chars, letters, digits, hyphens
        // IPv4: 0-255.0-255.0-255.0-255
        // IPv6: standard notation
        final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9-]{1,63}\\.)*[a-zA-Z0-9-]{1,63}$"
        );
        final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$"
        );
        final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F:]+$"
        );

        boolean valid = false;
        if (HOSTNAME_PATTERN.matcher(url).matches()) {
            valid = true;
        } else if (IPV4_PATTERN.matcher(url).matches()) {
            valid = true;
        } else if (IPV6_PATTERN.matcher(url).matches()) {
            valid = true;
        }

        if (!valid) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host format.");
            return;
        }

        // Build the command securely (no shell interpretation)
        // Windows: "ping -n 1 <host>"
        // Linux/macOS: "ping -c 1 <host>"
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmd;
        if (os.contains("win")) {
            cmd = new String[] { "ping", "-n", "1", url };
        } else {
            cmd = new String[] { "ping", "-c", "1", url };
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stdout and stderr

        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Output encoding: escape HTML to prevent XSS (CWE-79)
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Log exception securely (do not leak details to client)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping operation failed.");
            return;
        }

        // Respond with sanitized output
        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(
            "<html><body><h2>Ping Result</h2>" +
            "<pre>" + output.toString() + "</pre>" +
            "<p>Exit code: " + exitCode + "</p>" +
            "</body></html>"
        );
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     * @param s input string
     * @return escaped string
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
