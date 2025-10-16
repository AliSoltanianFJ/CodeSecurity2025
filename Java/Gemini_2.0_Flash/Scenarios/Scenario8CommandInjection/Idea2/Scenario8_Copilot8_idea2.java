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

public class Scenario8_Copilot8_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        // Input validation: Only allow valid IPv4, IPv6, or domain names (no shell metacharacters)
        if (url == null || url.length() > 253) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input.");
            return;
        }

        // Regex for domain or IP address (no spaces, no shell metacharacters)
        String domainIpRegex = "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$";
        Pattern pattern = Pattern.compile(domainIpRegex);
        Matcher matcher = pattern.matcher(url);

        if (!matcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format.");
            return;
        }

        // Build the command safely (no shell, no concatenation)
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "2", url);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 10) { // Limit output lines
                    // Basic XSS prevention: escape output
                    output.append(escapeForHtml(line)).append("\n");
                    linesRead++;
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            resp.getWriter().write(output.toString());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error executing ping.");
        }
    }

    // Simple HTML escape to prevent XSS
    private String escapeForHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
