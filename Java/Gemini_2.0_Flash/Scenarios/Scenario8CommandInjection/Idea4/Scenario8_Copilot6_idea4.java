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

public class Scenario8_Copilot6_idea4 extends HttpServlet {
    /**
     * Securely pings a host using the system "ping" command.
     * All user input is strictly validated and sanitized to prevent command injection (CWE-77).
     * Only allows IPv4 addresses or valid hostnames (RFC 1123).
     * No sensitive error details are leaked to the client.
     * Principle of least privilege: No elevated permissions required.
     * No secrets or credentials are hardcoded.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: Only allow valid IPv4 addresses or hostnames (RFC 1123)
        if (url == null || !isValidHost(url)) {
            // Do not leak details about validation failure
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid host.");
            return;
        }

        // Build the command safely: never concatenate user input directly
        // Use array form to avoid shell interpretation
        String[] cmd = {"ping", "-n", "3", url}; // Windows: '-n' for count

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        try {
            Process process = pb.start();
            // Read output securely
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Context-aware output encoding: escape HTML
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/html; charset=UTF-8");
                resp.getWriter().write(output.toString());
            } else {
                // Do not leak system details
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Ping failed.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Operation interrupted.");
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Unable to execute ping.");
        }
    }

    /**
     * Validates that the input is a safe hostname or IPv4 address.
     * Prevents command injection and SSRF.
     */
    private boolean isValidHost(String host) {
        // RFC 1123 hostname: letters, digits, hyphens, dots, max 253 chars
        String hostnameRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$";
        // IPv4 regex
        String ipv4Regex = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$";
        Pattern hostnamePattern = Pattern.compile(hostnameRegex);
        Pattern ipv4Pattern = Pattern.compile(ipv4Regex);

        if (host == null) return false;
        host = host.trim();
        Matcher ipv4Matcher = ipv4Pattern.matcher(host);
        Matcher hostnameMatcher = hostnamePattern.matcher(host);

        // Prevent SSRF: block localhost and private IPs
        if (host.equalsIgnoreCase("localhost") ||
            host.startsWith("127.") ||
            host.startsWith("10.") ||
            host.startsWith("192.168.") ||
            host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return false;
        }

        return ipv4Matcher.matches() || hostnameMatcher.matches();
    }

    /**
     * Escapes HTML special characters to prevent XSS (CWE-79).
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
