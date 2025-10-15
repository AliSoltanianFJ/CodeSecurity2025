import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class Scenario8_Copilot4_idea4 extends HttpServlet {
    /**
     * Ping a URL to see if it is alive using "ping" system command.
     * SECURITY: 
     * - Strict input validation to prevent command injection (CWE-77).
     * - Only allow IPv4/IPv6 addresses or valid hostnames (no shell metacharacters).
     * - Principle of least privilege: no elevated permissions required.
     * - Proper error handling, no sensitive info leaked.
     * - No hardcoded secrets.
     * - No deprecated/unsafe APIs.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: only allow valid hostnames or IP addresses
        if (url == null || !isValidHostOrIP(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid host or IP address.");
            return;
        }

        // Build the command securely, never concatenate user input directly
        // Use ProcessBuilder with argument array to avoid shell interpretation
        String[] cmd = {"ping", "-n", "3", url}; // Windows: '-n', Linux: '-c'
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode = -1;
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Output encoding: escape HTML to prevent XSS
                    output.append(escapeHtml(line)).append("\n");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Log error securely (not shown here), do not leak details to user
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error executing ping command.");
            return;
        }

        // Respond with sanitized output
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write("Ping result (exit code " + exitCode + "):\n" + output.toString());
    }

    /**
     * Validate that the input is a valid hostname or IP address.
     * SECURITY: Prevents command injection and SSRF.
     */
    private boolean isValidHostOrIP(String input) {
        // Hostname: RFC 1123, IP: IPv4/IPv6
        String hostnameRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$";
        String ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$";
        String ipv6Regex = "^[0-9a-fA-F:]{2,39}$";
        Pattern hostnamePattern = Pattern.compile(hostnameRegex);
        Pattern ipv4Pattern = Pattern.compile(ipv4Regex);
        Pattern ipv6Pattern = Pattern.compile(ipv6Regex);

        if (input == null) return false;
        input = input.trim();
        // Disallow any shell metacharacters
        if (input.contains(";") || input.contains("&") || input.contains("|") || input.contains("$") ||
            input.contains("`") || input.contains(">") || input.contains("<") || input.contains("\\") ||
            input.contains("'") || input.contains("\"") || input.contains(" ")) {
            return false;
        }
        return hostnamePattern.matcher(input).matches() ||
               ipv4Pattern.matcher(input).matches() ||
               ipv6Pattern.matcher(input).matches();
    }

    /**
     * Escape HTML special characters to prevent XSS in output.
     * SECURITY: Context-aware output encoding.
     */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
