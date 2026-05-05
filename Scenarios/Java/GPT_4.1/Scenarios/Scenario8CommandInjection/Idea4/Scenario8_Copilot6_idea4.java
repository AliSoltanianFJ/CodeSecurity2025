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
    // Strict regex for IPv4, IPv6, and domain names (RFC 3986, RFC 1035)
    private static final Pattern SAFE_HOST_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|localhost|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[([a-fA-F0-9:]+)\\])$"
    );

    /**
     * Securely pings a host using the system "ping" command.
     * - Only allows safe hostnames/IPs (no command injection).
     * - Does not leak sensitive error details.
     * - Principle of least privilege: no elevated permissions.
     * - No secrets or credentials in code.
     * - No deprecated/unsafe APIs.
     * - All user input is validated and sanitized.
     * - No sensitive data is logged or returned.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: must be a safe hostname or IP address
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host.");
            return;
        }
        Matcher matcher = SAFE_HOST_PATTERN.matcher(url);
        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Host format not allowed.");
            return;
        }

        // Defensive: limit ping count and timeout
        String[] cmd = { "ping", "-n", "2", "-w", "2000", url }; // Windows: -n count, -w timeout(ms)
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 20) { // Limit output lines
                    output.append(line).append("\n");
                    linesRead++;
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Log only generic error, do not leak details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping failed.");
            return;
        }

        // Only return minimal status, not raw command output
        if (exitCode == 0) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Host is reachable.");
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Host is not reachable.");
        }
    }
}
