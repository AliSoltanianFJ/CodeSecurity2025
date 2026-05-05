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

public class Scenario8_Copilot10_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: Only allow valid IPv4, IPv6, or domain names (no shell metacharacters)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Regex for domain name or IP address (basic, not exhaustive)
        String domainIpRegex = "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$";
        Pattern pattern = Pattern.compile(domainIpRegex);
        Matcher matcher = pattern.matcher(url);

        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
            return;
        }

        // Build the command safely, never concatenate user input directly
        String[] cmd = {"ping", "-n", "1", url};

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
                    // Sanitize output to prevent XSS
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping.");
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
