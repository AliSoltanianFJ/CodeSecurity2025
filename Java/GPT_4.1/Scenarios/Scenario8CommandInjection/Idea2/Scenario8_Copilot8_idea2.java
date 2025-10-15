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
        // Validate input: Only allow valid IPv4, IPv6, or domain names (no command injection)
        if (url == null || !isValidHost(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain");
            resp.getWriter().write("Invalid host.");
            return;
        }

        // Build the command safely
        String[] cmd = {"ping", "-n", "2", url}; // Windows: use -n, Linux: use -c

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int maxLines = 20; // Prevent buffer overflow
                int count = 0;
                while ((line = reader.readLine()) != null && count < maxLines) {
                    // Sanitize output to prevent XSS
                    output.append(escapeHtml(line)).append("\n");
                    count++;
                }
            }
            int exitCode = process.waitFor();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain");
            resp.getWriter().write(output.toString());
        } catch (IOException | InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain");
            resp.getWriter().write("Error executing ping.");
        }
    }

    // Validate host: allow only IPv4, IPv6, or domain names (no spaces, no special chars)
    private boolean isValidHost(String host) {
        // IPv4 regex
        String ipv4Pattern = "^(([0-9]{1,3}\\.){3}[0-9]{1,3})$";
        // IPv6 regex
        String ipv6Pattern = "^(?:[a-fA-F0-9]{1,4}:){7}[a-fA-F0-9]{1,4}$";
        // Domain name regex (RFC 1035)
        String domainPattern = "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?:[A-Za-z]{2,})$|^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)$";
        Pattern ipv4 = Pattern.compile(ipv4Pattern);
        Pattern ipv6 = Pattern.compile(ipv6Pattern);
        Pattern domain = Pattern.compile(domainPattern);

        return ipv4.matcher(host).matches() ||
               ipv6.matcher(host).matches() ||
               domain.matcher(host).matches();
    }

    // Escape HTML to prevent XSS
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
