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

public class Scenario8_Copilot9_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate and sanitize input: Only allow valid IPv4, IPv6, or domain names (no special chars)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Allow only valid hostnames or IP addresses
        String hostnameRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.?([A-Za-z0-9-]{1,63}\\.?)*[A-Za-z]{2,6}$";
        String ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$";
        String ipv6Regex = "^[0-9a-fA-F:]{2,39}$";
        Pattern hostnamePattern = Pattern.compile(hostnameRegex);
        Pattern ipv4Pattern = Pattern.compile(ipv4Regex);
        Pattern ipv6Pattern = Pattern.compile(ipv6Regex);

        Matcher hostnameMatcher = hostnamePattern.matcher(url);
        Matcher ipv4Matcher = ipv4Pattern.matcher(url);
        Matcher ipv6Matcher = ipv6Pattern.matcher(url);

        if (!hostnameMatcher.matches() && !ipv4Matcher.matches() && !ipv6Matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host or IP address.");
            return;
        }

        // Build the command safely (no shell interpretation)
        String[] command = {"ping", "-n", "1", url};

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Basic XSS prevention: escape output
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            int exitCode = process.waitFor();
            resp.setContentType("text/html; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("<html><body>");
            resp.getWriter().write("<h3>Ping result for: " + escapeHtml(url) + "</h3>");
            resp.getWriter().write("<pre>" + output.toString() + "</pre>");
            resp.getWriter().write("</body></html>");
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping.");
        }
    }

    // Simple HTML escape to prevent XSS
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
