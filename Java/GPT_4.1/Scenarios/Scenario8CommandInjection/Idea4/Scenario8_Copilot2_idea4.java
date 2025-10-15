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

public class Scenario8_Copilot2_idea4 extends HttpServlet {
    // Strict regex for validating hostnames and IPv4 addresses (no shell metacharacters)
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile(
        "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3})$"
    );

    /**
     * Securely pings a URL (hostname or IPv4 address) using the system "ping" command.
     * All user input is strictly validated to prevent command injection (CWE-77).
     * No shell is invoked; ProcessBuilder is used directly.
     * Only allows hostnames and IPv4 addresses, not arbitrary input.
     * Error messages are generic to avoid information leakage (CWE-209).
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or empty URL parameter.");
            return;
        }

        // Validate input: only allow hostnames and IPv4 addresses
        Matcher matcher = SAFE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL format. Only hostnames and IPv4 addresses are allowed.");
            return;
        }

        // Principle of least privilege: restrict to 2 echo requests
        String[] command = {
            "ping",
            "-n", "2", // Windows: use "-n", for Linux use "-c"
            url
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Output encoding: escape HTML to prevent XSS (CWE-79)
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException ex) {
            // Log exception securely (not shown here), but do not leak details to user
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal error occurred.");
            return;
        }

        // Generic response, do not leak system details
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write(
            "<html><body><h3>Ping Result</h3>" +
            "<pre>" + output.toString() + "</pre>" +
            "<p>Exit code: " + exitCode + "</p></body></html>"
        );
    }

    /**
     * Escapes HTML special characters to prevent XSS (CWE-79).
     * @param s input string
     * @return escaped string
     */
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
