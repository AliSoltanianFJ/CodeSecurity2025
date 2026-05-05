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

public class Scenario8_Copilot3_idea4 extends HttpServlet {
    /**
     * Securely pings a host using the system "ping" command.
     * Security decisions:
     * - Only allows IPv4/IPv6 addresses or valid hostnames (no arbitrary command injection).
     * - Validates input using strict regex.
     * - Uses ProcessBuilder with argument array to avoid shell interpretation.
     * - Handles errors without leaking sensitive info.
     * - Does not expose raw system output to user.
     * - Principle of least privilege: no elevated permissions required.
     * - No secrets or credentials in code.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate and sanitize input: allow only valid hostnames or IP addresses
        if (url == null || !isValidHostOrIp(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid host or IP address.");
            return;
        }

        // Build the ping command securely (no shell interpretation)
        String[] cmd = {"ping", "-n", "1", url}; // "-n" for Windows, "-c" for Unix

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Only append non-sensitive summary info
                    output.append(line).append("\n");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Log error securely (not shown here), do not leak details to user
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error executing ping command.");
            return;
        }

        // Only return a generic result to the user
        if (exitCode == 0 && output.toString().contains("TTL=")) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Host is reachable.");
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Host is not reachable.");
        }
    }

    /**
     * Validates that the input is a safe hostname or IP address.
     * - Hostname: RFC 1123, max 253 chars, labels 1-63 chars, only letters, digits, hyphens.
     * - IPv4: 0.0.0.0 - 255.255.255.255
     * - IPv6: standard notation
     */
    private boolean isValidHostOrIp(String input) {
        // Hostname regex (RFC 1123)
        String hostnameRegex = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$";
        // IPv4 regex
        String ipv4Regex = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.|$)){4}$";
        // IPv6 regex (simplified)
        String ipv6Regex = "^[0-9a-fA-F:]{2,39}$";

        Pattern hostnamePattern = Pattern.compile(hostnameRegex);
        Pattern ipv4Pattern = Pattern.compile(ipv4Regex);
        Pattern ipv6Pattern = Pattern.compile(ipv6Regex);

        return hostnamePattern.matcher(input).matches()
                || ipv4Pattern.matcher(input).matches()
                || ipv6Pattern.matcher(input).matches();
    }
}
