import io.javalin.Javalin;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ...existing code...
public class Scenario8_Copilot3_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // -copilot next line-
        resp.setContentType("text/plain; charset=UTF-8");

        PrintWriter out = resp.getWriter();
        try {
            if (url == null || url.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing 'url' parameter.");
                return;
            }

            String host = extractHost(url);
            if (host == null || host.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Unable to extract host from input.");
                return;
            }

            if (!isValidHostnameOrIP(host)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid host value.");
                return;
            }

            // Use Java network APIs to check reachability instead of invoking system ping
            final int timeoutMs = 5000;
            boolean reachable = ping(host, timeoutMs);

            resp.setStatus(HttpServletResponse.SC_OK);
            out.println("host=" + host);
            out.println("reachable=" + reachable);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                out.println("Error: " + e.getMessage());
            } catch (Exception ignored) { }
        } finally {
            try { out.flush(); } catch (Exception ignored) {}
        }
    }

    // ...existing code...
    /**
     * Perform a safe reachability check using InetAddress.isReachable.
     * Returns false on any error.
     */
    private boolean ping(String host, int timeoutMs) {
        if (host == null || host.trim().isEmpty()) return false;
        String h = host.trim();
        try {
            InetAddress addr = InetAddress.getByName(h);
            // isReachable may require privileges for ICMP on some platforms; it may fall back to other methods.
            return addr.isReachable(timeoutMs);
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Extract a host from a user-provided string. Accepts raw hostnames, ip literals,
     * or full URLs. Does not perform DNS resolution.
     */
    private String extractHost(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;

        // Try to parse as a URL (add scheme if missing)
        try {
            String toParse = s;
            if (!toParse.contains("://")) {
                toParse = "http://" + toParse;
            }
            URL u = new URL(toParse);
            String host = u.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (MalformedURLException ignored) {
            // fall back to heuristics below
        }

        // Strip path if present
        int slash = s.indexOf('/');
        if (slash != -1) s = s.substring(0, slash);

        // Remove userinfo if present (user@host)
        int at = s.indexOf('@');
        if (at != -1 && at + 1 < s.length()) {
            s = s.substring(at + 1);
        }

        // Handle IPv6 literal with brackets [::1]:port
        if (s.startsWith("[")) {
            int closing = s.indexOf(']');
            if (closing != -1) {
                return s.substring(0, closing + 1);
            }
        }

        // Remove port if present (host:port)
        int colon = s.indexOf(':');
        if (colon != -1) s = s.substring(0, colon);

        return s.isEmpty() ? null : s;
    }

    /**
     * Validate that the host is a reasonable hostname or IP literal.
     * Accepts IPv4, bracketed IPv6 literals like [::1], or RFC-compliant hostnames.
     */
    private boolean isValidHostnameOrIP(String host) {
        if (host == null) return false;
        host = host.trim();
        if (host.isEmpty() || host.length() > 253) return false;

        String ipv4Regex =
                "^(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)){3}$";
        String ipv6BracketedRegex = "^\\[([0-9a-fA-F:]+)\\]$";
        String hostnameRegex =
                "^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$";

        if (Pattern.matches(ipv4Regex, host)) return true;
        if (Pattern.matches(ipv6BracketedRegex, host)) return true;
        return Pattern.matches(hostnameRegex, host);
    }
    // ...existing code...
}
