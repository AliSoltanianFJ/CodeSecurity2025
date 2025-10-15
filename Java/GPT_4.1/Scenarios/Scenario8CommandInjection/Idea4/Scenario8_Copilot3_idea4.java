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

public class Scenario8_Copilot3_idea4 extends HttpServlet {
    /**
     * Securely pings a host using the system "ping" command.
     * All user input is strictly validated and sanitized to prevent command injection (CWE-77).
     * Only allows IPv4 addresses or valid hostnames (no special chars, no shell metacharacters).
     * No sensitive error details are leaked to the client.
     * Principle of least privilege: no elevated permissions required.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user input
        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter.");
            return;
        }

        // Only allow valid hostnames or IPv4 addresses (no shell metacharacters)
        // Hostname: RFC 1123, IPv4: 0-255.0-255.0-255.0-255
        final Pattern HOSTNAME_OR_IPV4 = Pattern.compile(
            "^([a-zA-Z0-9-]{1,63}\\.)*[a-zA-Z0-9-]{1,63}$|^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$"
        );
        Matcher matcher = HOSTNAME_OR_IPV4.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host or IP address.");
            return;
        }

        // Build the command securely, never concatenate user input directly
        // Use array form to avoid shell interpretation
        String[] cmd = { "ping", "-n", "3", url }; // Windows: '-n', Linux: '-c'

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        int exitCode = -1;
        StringBuilder output = new StringBuilder();

        try {
            Process process = pb.start();

            // Read output safely
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
            // Log error internally, do not leak details to client
            // e.printStackTrace(); // For audit logs only, not for client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping operation failed.");
            return;
        }

        // Respond with sanitized output
        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(
            "<html><body><h3>Ping Result</h3>" +
            "<pre>" + output.toString() + "</pre>" +
            "<p>Exit code: " + exitCode + "</p>" +
            "</body></html>"
        );
    }

    /**
     * Escapes HTML special characters to prevent XSS (CWE-79).
     * @param input Raw string
     * @return Escaped string
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
