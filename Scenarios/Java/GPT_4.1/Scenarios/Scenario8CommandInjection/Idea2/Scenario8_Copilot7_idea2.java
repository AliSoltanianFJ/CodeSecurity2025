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

public class Scenario8_Copilot7_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate and sanitize input: Only allow IPv4, IPv6, or domain names (no shell metacharacters)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Only allow valid domain names or IP addresses
        String domainRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?:[A-Za-z]{2,})$";
        String ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$";
        String ipv6Regex = "^[0-9a-fA-F:]+$";
        Pattern domainPattern = Pattern.compile(domainRegex);
        Pattern ipv4Pattern = Pattern.compile(ipv4Regex);
        Pattern ipv6Pattern = Pattern.compile(ipv6Regex);

        Matcher domainMatcher = domainPattern.matcher(url);
        Matcher ipv4Matcher = ipv4Pattern.matcher(url);
        Matcher ipv6Matcher = ipv6Pattern.matcher(url);

        if (!domainMatcher.matches() && !ipv4Matcher.matches() && !ipv6Matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
            return;
        }

        // Build the command safely (no shell interpretation)
        String[] cmd;
        if (ipv6Matcher.matches()) {
            cmd = new String[]{"ping", "-n", "1", "-6", url}; // Windows IPv6
        } else {
            cmd = new String[]{"ping", "-n", "1", url}; // Windows IPv4/domain
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode = -1;
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Prevent XSS by escaping output
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping command.");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store");

        resp.getWriter().write("<html><body>");
        resp.getWriter().write("<h2>Ping Result</h2>");
        resp.getWriter().write("<pre>");
        resp.getWriter().write(output.toString());
        resp.getWriter().write("</pre>");
        resp.getWriter().write("<p>Exit code: " + exitCode + "</p>");
        resp.getWriter().write("</body></html>");
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
