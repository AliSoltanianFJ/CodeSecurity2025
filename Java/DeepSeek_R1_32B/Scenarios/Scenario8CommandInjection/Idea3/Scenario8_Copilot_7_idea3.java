import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot_7_idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty URL parameter.");
            return;
        }

        // Validate input: Only allow hostnames with alphanumeric, hyphens, dots and slashes
        if (!isValidHost(url)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
            return;
        }

        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", url);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null && output.length() < 1024) { // Limit output size
                    output.append(line).append(System.lineSeparator());
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.setContentType("text/plain");
                resp.getWriter().write(sanitizeOutput(output.toString()));
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping failed.");
            }
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing request.");
        }
    }

    // Validate hostnames to prevent command injection
    private boolean isValidHost(String host) {
        return host.matches("^[a-zA-Z0-9.-]{1,253}$");
    }

    // Sanitize output to remove potentially sensitive information
    private String sanitizeOutput(String output) {
        // Remove any timing information or other sensitive details
        return output.replaceAll("\\d+ bytes", "X bytes").replaceAll("\\b\\d+ ms\\b", "X ms");
    }
}