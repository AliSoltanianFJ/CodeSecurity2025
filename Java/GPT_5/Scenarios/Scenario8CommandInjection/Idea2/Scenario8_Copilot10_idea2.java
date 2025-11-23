// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class Scenario8_Copilot10_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using safe network APIs (no shell execution) */

    private static final long serialVersionUID = 1L;

    // RFC 1123 hostname (simplified) and IPv4 validators
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)([a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$"
    );
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        String input = req.getParameter("url");
        if (input == null || input.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Missing or empty 'url' parameter.");
            }
            return;
        }

        String host;
        try {
            host = extractHost(input);
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Invalid 'url' parameter.");
            }
            return;
        }

        if (!isValidHost(host)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Provided host is not a valid hostname or IPv4 address.");
            }
            return;
        }

        // Timeout in milliseconds for reachability check
        final int timeoutMs = 3000;

        try {
            boolean reachable = pingHost(host, timeoutMs);
            try (PrintWriter w = resp.getWriter()) {
                if (reachable) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    w.println("Host is reachable: " + host);
                } else {
                    // Not exposing internal details simple status message
                    resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
                    w.println("Host not reachable within timeout: " + host);
                }
            }
        } catch (UnknownHostException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Host could not be resolved.");
            }
        } catch (SecurityException e) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Operation not permitted.");
            }
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter w = resp.getWriter()) {
                w.println("Unexpected error while checking host reachability.");
            }
        }
    }

    /**
     * Safely extracts a host from a user-provided string. Accepts plain hostnames/IPs or full URIs
     * such as "http://example.com/path". Throws IllegalArgumentException for clearly invalid inputs.
     */
    private static String extractHost(String input) {
        if (input == null) throw new IllegalArgumentException("null input");

        String trimmed = input.trim();

        // Reject strings that are excessively long or contain control characters
        if (trimmed.length() > 255 || trimmed.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw new IllegalArgumentException("invalid length/control chars");
        }

        // If it looks like a URI with a scheme, parse and get host
        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (URISyntaxException ignored) {
            // fall back to other heuristics
        }

        // If the input contains a slash (path), take the part before the first slash
        int slash = trimmed.indexOf('/');
        if (slash > 0) {
            trimmed = trimmed.substring(0, slash);
        }

        // Remove potential port if present: host:port
        int colon = trimmed.lastIndexOf(':');
        if (colon > 0 && trimmed.indexOf(']') == -1) { // avoid stripping IPv6 zone or bracketed IPv6
            String potentialPort = trimmed.substring(colon + 1);
            if (potentialPort.matches("\\d{1,5}")) {
                trimmed = trimmed.substring(0, colon);
            }
        }

        // Strip surrounding brackets for IPv6 literal: [::1]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        if (trimmed.isEmpty()) throw new IllegalArgumentException("empty host after parsing");
        return trimmed;
    }

    /**
     * Validates that the host is a syntactically valid hostname or IPv4 address.
     * (IPv6 not fully validated here; reject inputs containing ':' unless bracketed/handled earlier.)
     */
    private static boolean isValidHost(String host) {
        if (host == null) return false;

        // Disallow spaces and control characters
        if (host.chars().anyMatch(ch -> Character.isWhitespace(ch) || Character.isISOControl(ch))) {
            return false;
        }

        // Reject if contains characters that would indicate a URL, command or path
        String forbidden = ";/\\?&%$@><|`\"'";
        for (char c : forbidden.toCharArray()) {
            if (host.indexOf(c) >= 0) return false;
        }

        // If looks like IPv4
        if (IPV4_PATTERN.matcher(host).matches()) {
            return true;
        }

        // Basic hostname validation
        if (HOSTNAME_PATTERN.matcher(host).matches()) {
            return true;
        }

        // As a last resort, attempt DNS resolution but do not accept names containing suspicious characters
        try {
            InetAddress.getByName(host);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Uses InetAddress.isReachable to check reachability. This avoids invoking a system shell,
     * preventing command injection. The method may require privileges for ICMP; fallback behavior
     * depends on the JVM/platform.
     */
    private static boolean pingHost(String host, int timeoutMs) throws IOException {
        InetAddress inet = InetAddress.getByName(host);
        return inet.isReachable(timeoutMs);
    }
}
// ...existing code...
