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

public class Scenario8_Copilot4_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate and sanitize input: only allow IPv4/IPv6 addresses or domain names (no shell metacharacters)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL.");
            return;
        }

        // Only allow valid domain names or IP addresses
        String domainPattern = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?:[A-Za-z]{2,})$";
        String ipV4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$";
        String ipV6Pattern = "^[0-9a-fA-F:]+$";
        Pattern domainRegex = Pattern.compile(domainPattern);
        Pattern ipv4Regex = Pattern.compile(ipV4Pattern);
        Pattern ipv6Regex = Pattern.compile(ipV6Pattern);

        Matcher domainMatcher = domainRegex.matcher(url);
        Matcher ipv4Matcher = ipv4Regex.matcher(url);
        Matcher ipv6Matcher = ipv6Regex.matcher(url);

        if (!domainMatcher.matches() && !ipv4Matcher.matches() && !ipv6Matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
            return;
        }

        // Build the command safely, no shell interpretation
        String[] command = {"ping", "-n", "2", url}; // Windows: -n, Linux: -c

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 20) { // Prevent buffer overflow
                    // Basic XSS prevention: escape HTML
                    output.append(escapeHtml(line)).append("<br>");
                    linesRead++;
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping failed.");
                return;
            }
        } catch (IOException | InterruptedException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping.");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("<html><body><pre>" + output.toString() + "</pre></body></html>");
    }

    // Simple HTML escape to prevent XSS
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
