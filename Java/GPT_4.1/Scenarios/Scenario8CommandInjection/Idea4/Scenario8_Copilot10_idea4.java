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

public class Scenario8_Copilot10_idea4 extends HttpServlet {
    /**
     * Securely pings a URL to see if it is alive using the "ping" system command.
     * All user input is strictly validated and sanitized to prevent command injection (CWE-77).
     * Only allows IPv4/IPv6 addresses or valid hostnames, and limits the number of ping packets.
     * No sensitive error details are leaked to the client.
     * Principle of least privilege: No elevated permissions required.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize the 'url' parameter
        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty URL parameter.");
            return;
        }

        // Only allow valid hostnames or IP addresses (strict regex)
        // Hostname: RFC 1123, IPv4, IPv6
        final Pattern VALID_HOSTNAME_OR_IP = Pattern.compile(
            "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|"
            + "(?:\\d{1,3}\\.){3}\\d{1,3}|"
            + "\\[[0-9a-fA-F:]+\\])$"
        );
        Matcher matcher = VALID_HOSTNAME_OR_IP.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format. Only hostnames or IP addresses are allowed.");
            return;
        }

        // Limit the number of ping packets for safety
        final int PING_COUNT = 2;

        // Build the command securely, avoiding shell interpretation
        // Use array form to prevent command injection (no shell parsing)
        String[] cmd;
        if (url.startsWith("[")) { // IPv6
            cmd = new String[] { "ping", "-n", String.valueOf(PING_COUNT), url };
        } else {
            cmd = new String[] { "ping", "-n", String.valueOf(PING_COUNT), url };
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        StringBuilder output = new StringBuilder();
        int exitCode = -1;
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
        } catch (IOException | InterruptedException ex) {
            // Log error securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping operation failed.");
            return;
        }

        // Respond with the ping result (do not leak sensitive system info)
        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("<html><body>");
        resp.getWriter().write("<h3>Ping result for: " + escapeHtml(url) + "</h3>");
        resp.getWriter().write("<pre>" + output.toString() + "</pre>");
        resp.getWriter().write("<p>Exit code: " + exitCode + "</p>");
        resp.getWriter().write("</body></html>");
    }

    /**
     * Escapes HTML special characters to prevent XSS (CWE-79).
     * @param input The string to escape.
     * @return The escaped string.
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
