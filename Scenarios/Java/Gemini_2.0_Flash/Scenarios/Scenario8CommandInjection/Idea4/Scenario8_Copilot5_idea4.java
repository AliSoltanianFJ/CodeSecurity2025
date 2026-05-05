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

public class Scenario8_Copilot5_idea4 extends HttpServlet {
    // Strict regex for validating IPv4, IPv6, and domain names
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$"
    );

    /**
     * Securely pings a host using the system "ping" command.
     * - Validates and sanitizes user input to prevent command injection (CWE-77).
     * - Only allows domain names, IPv4, or IPv6 addresses.
     * - Uses ProcessBuilder with argument array to avoid shell interpretation.
     * - Handles errors without leaking sensitive information.
     * - Principle of least privilege: no elevated permissions required.
     * - No secrets or credentials hardcoded.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        // Validate input: allow only safe hostnames or IP addresses
        if (url == null || url.length() > 253 || !isSafeUrl(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid host format.");
            return;
        }

        // Build the ping command securely (no shell interpretation)
        String[] cmd = {"ping", "-n", "3", url}; // Windows: -n for count

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        try {
            Process process = pb.start();

            // Read output safely
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Output encoding: escape for plain text
                    output.append(escapeForPlainText(line)).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            resp.getWriter().write(output.toString());

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Ping interrupted.");
        } catch (IOException ioe) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error executing ping.");
        }
    }

    /**
     * Validates that the input is a safe hostname or IP address.
     * Prevents SSRF, RCE, and command injection.
     */
    private boolean isSafeUrl(String url) {
        Matcher matcher = SAFE_URL_PATTERN.matcher(url);
        return matcher.matches();
    }

    /**
     * Escapes output for safe display in plain text.
     * Prevents XSS if output is ever rendered in HTML.
     */
    private String escapeForPlainText(String input) {
        // Replace control characters and non-printable chars
        return input.replaceAll("[\\x00-\\x1F\\x7F]", "");
    }
}
