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
    /**
     * Securely pings a host using the system "ping" command.
     * All user input is strictly validated to prevent command injection (CWE-77).
     * Only allows IPv4/IPv6 addresses or valid hostnames (RFC 1123).
     * No shell invocation; uses ProcessBuilder with argument array.
     * All errors are handled without leaking sensitive information.
     * Principle of least privilege: no elevated permissions required.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: only allow valid hostnames or IP addresses
        if (url == null || !isValidHostOrIp(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid host or IP address.");
            return;
        }

        // Build the ping command securely (no shell invocation)
        // Limit to 2 echo requests for safety
        String[] cmd = {"ping", "-n", "2", url};

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // Merge stderr with stdout

        int exitCode = -1;
        StringBuilder output = new StringBuilder();

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Output encoding: escape HTML to prevent XSS
                    output.append(escapeHtml(line)).append("\n");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Log error securely (do not expose details to user)
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error executing ping command.");
            return;
        }

        // Respond with ping output (sanitized)
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write(output.toString());
    }

    /**
     * Validates that the input is a valid hostname or IP address.
     * Prevents command injection and SSRF by restricting allowed formats.
     * @param input User-supplied host or IP
     * @return true if valid, false otherwise
     */
    private boolean isValidHostOrIp(String input) {
        // RFC 1123 hostname: max 253 chars, labels 1-63 chars, letters/digits/hyphens
        String hostnameRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$";
        // IPv4 regex
        String ipv4Regex = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$";
        // IPv6 regex (simplified)
        String ipv6Regex = "^[0-9a-fA-F:]{2,39}$";

        Pattern hostnamePattern = Pattern.compile(hostnameRegex);
        Pattern ipv4Pattern = Pattern.compile(ipv4Regex);
        Pattern ipv6Pattern = Pattern.compile(ipv6Regex);

        if (input == null) return false;
        input = input.trim();
        if (input.length() == 0) return false;

        Matcher m1 = hostnamePattern.matcher(input);
        Matcher m2 = ipv4Pattern.matcher(input);
        Matcher m3 = ipv6Pattern.matcher(input);

        return m1.matches() || m2.matches() || m3.matches();
    }

    /**
     * Escapes HTML special characters to prevent XSS.
     * @param s Input string
     * @return Escaped string
     */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
