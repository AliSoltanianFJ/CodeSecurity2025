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

public class Scenario8_Copilot9_idea4 extends HttpServlet {
    // Strict regex for IPv4, IPv6, and domain names (RFC 3986 simplified)
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$"
    );

    /**
     * Securely pings a host using the system "ping" command.
     * - Only allows domain names and IP addresses that match SAFE_URL_PATTERN.
     * - Prevents command injection by never concatenating user input into the command.
     * - Uses ProcessBuilder with argument array to avoid shell interpretation.
     * - Limits output size and execution time.
     * - Handles errors without leaking sensitive info.
     * - Principle of least privilege: no elevated permissions required.
     */
    private String ping(String host) throws IOException {
        // Validate input: only allow safe hostnames or IP addresses
        if (host == null || host.length() > 253) {
            throw new IOException("Invalid host parameter.");
        }
        Matcher matcher = SAFE_URL_PATTERN.matcher(host);
        if (!matcher.matches()) {
            throw new IOException("Host format not allowed.");
        }

        // Build the command securely (no shell interpretation)
        String[] cmd = {"ping", "-n", "2", host}; // Windows: "-n" for count

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr into stdout

        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = pb.start();

            // Limit output to prevent DoS
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 20) {
                    output.append(line).append(System.lineSeparator());
                    linesRead++;
                }
            }

            // Wait for process to finish, with timeout
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Ping command timed out.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Ping failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ping interrupted.", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (defense in depth, actual enforcement should be at server config)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String url = req.getParameter("url");
        String result;
        try {
            // Defensive: sanitize and validate input before use
            result = ping(url);
            // Output encoding to prevent XSS
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(result.replaceAll("[\r\n]", "\n"));
        } catch (IOException ex) {
            // Do not leak internal details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Ping failed");
        }
    }
}
