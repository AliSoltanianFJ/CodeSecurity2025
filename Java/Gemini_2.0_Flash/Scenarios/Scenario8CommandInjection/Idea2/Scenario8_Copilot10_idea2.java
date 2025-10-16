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
        String result = "";

        // Validate input: Only allow IPv4, IPv6, or domain names (no special chars)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Only allow safe hostnames or IP addresses
        Pattern safePattern = Pattern.compile("^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$");
        Matcher matcher = safePattern.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL must be a valid hostname or IP address.");
            return;
        }

        // Build the command safely (no concatenation)
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", url);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 10) { // Prevent buffer overflow
                    // Basic output sanitization to prevent XSS
                    output.append(line.replaceAll("[<>]", "")).append("\n");
                    linesRead++;
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                result = "Ping successful:\n" + output.toString();
            } else {
                result = "Ping failed:\n" + output.toString();
            }
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping.");
            return;
        }

        // Set content type and write sanitized output
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write(result);
    }
}
