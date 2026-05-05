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

public class Scenario8_Copilot5_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        String result = "";

        // Validate and sanitize input: Only allow IPv4, IPv6, or valid hostname (no special chars)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Hostname: RFC 1123, IPv4, IPv6
        Pattern safePattern = Pattern.compile(
            "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$"
        );
        Matcher matcher = safePattern.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL format not allowed.");
            return;
        }

        // Build command safely (no shell interpretation)
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
                    // Prevent XSS by escaping output
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                result = "Ping successful:<br>" + output.toString();
            } else {
                result = "Ping failed:<br>" + output.toString();
            }
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping.");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(result);
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
