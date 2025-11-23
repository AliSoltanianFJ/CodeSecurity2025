// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// ...existing code...
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class Scenario8_Copilot5_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            if (url == null || url.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing 'url' parameter.");
                return;
            }

            String host = url.trim();

            // Try to extract host from a URL if a scheme or path is present.
            try {
                if (!host.contains("://")) {
                    // If there's a path-like part, try to parse by prepending a scheme.
                    URL parsed = new URL("http://" + host);
                    if (parsed.getHost() != null && !parsed.getHost().isEmpty()) {
                        host = parsed.getHost();
                    }
                } else {
                    URL parsed = new URL(host);
                    if (parsed.getHost() != null && !parsed.getHost().isEmpty()) {
                        host = parsed.getHost();
                    }
                }
            } catch (MalformedURLException ignored) {
                // If parsing fails, fall back to treating the original input as a hostname/IP.
            }

            // Remove surrounding brackets for IPv6 literal (if provided)
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }

            // If host includes a port (e.g., example.com:8080) remove the port part for reachability check.
            // For IPv6 without brackets this will be ambiguous; common inputs use brackets for IPv6+port.
            int colonIndex = host.indexOf(':');
            if (colonIndex != -1 && !host.contains(".")) {
                // Heuristic: if there is a colon and no dot, this may be an IPv6 address; keep as-is.
            } else if (colonIndex != -1 && host.split(":").length == 2 && host.matches("^[^\\[\\]]+:\\d+$")) {
                host = host.substring(0, colonIndex);
            }

            if (host.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Unable to extract host from input.");
                return;
            }

            if (!isValidHostnameOrIP(host)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid host value.");
                return;
            }

            InetAddress addr;
            try {
                addr = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Unknown host: " + host);
                return;
            }

            // Use isReachable instead of launching system ping to avoid command-injection risks.
            final int timeoutMs = 5000;
            boolean reachable;
            try {
                reachable = addr.isReachable(timeoutMs);
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("I/O error while checking reachability: " + e.getMessage());
                return;
            } catch (SecurityException e) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.println("Security manager prevented reachability check.");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            if (reachable) {
                out.println("Host is reachable: " + host);
            } else {
                out.println("Host is NOT reachable: " + host);
            }
        }
    }

    // Validate that the host is a reasonable hostname or IP literal.
    private boolean isValidHostnameOrIP(String host) {
        if (host == null) return false;
        host = host.trim();
        if (host.isEmpty() || host.length() > 255) return false;

        // IPv4 pattern
        Pattern ipv4 = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)(\\.|$)){4}$");
        if (ipv4.matcher(host).matches()) {
            // further numeric range checks handled by regex
            return true;
        }

        // Simple IPv6 accept (hex digits and colon). Accept zone id (%) too (common on Windows/Unix).
        Pattern ipv6 = Pattern.compile("^[0-9a-fA-F:]+(%[0-9a-zA-Z]+)?$");
        if (ipv6.matcher(host).matches()) {
            return true;
        }

        // Hostname: letters, digits, hyphen, dot. Labels 1..63, not start/end with hyphen.
        if (!host.matches("^[A-Za-z0-9.-]+$")) return false;
        String[] labels = host.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) return false;
            if (label.startsWith("-") || label.endsWith("-")) return false;
        }
        return true;
    }
}
// ...existing code...
