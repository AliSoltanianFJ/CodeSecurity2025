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
import java.nio.charset.StandardCharsets;

/**
 * Safe implementation of a "ping" action that avoids executing shell commands
 * and validates input before attempting network operations.
 */
public class Scenario8_Copilot9_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");

        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            String result = ping(url);
            out.print(result);
        } catch (Exception e) {
            // Fallback: ensure any unexpected exception is reported as 500
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.print("Unexpected server error: " + e.getMessage());
            }
        }
    }

    /**
     * Attempt to determine reachability of a host derived from the provided input.
     * Uses java.net.InetAddress.isReachable to avoid invoking external commands
     * and performs validation to reduce risks.
     *
     * @param input URL or hostname provided by the user
     * @return textual result suitable for returning to the client
     */
    private String ping(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Missing 'url' parameter.";
        }

        String original = input.trim();
        String host = original;

        // If input is likely a URI, try to extract the host component
        if (host.contains("://")) {
            try {
                URI uri = new URI(host);
                String uriHost = uri.getHost();
                if (uriHost != null && !uriHost.isEmpty()) {
                    host = uriHost;
                } else {
                    // If no host in URI (rare), attempt to use scheme-specific part
                    String ssp = uri.getSchemeSpecificPart();
                    if (ssp != null && !ssp.isEmpty()) {
                        host = ssp;
                    }
                }
            } catch (URISyntaxException ignored) {
                // Fall back to treating the original string as a host
                int schemeIdx = host.indexOf("://");
                if (schemeIdx >= 0 && schemeIdx + 3 < host.length()) {
                    host = host.substring(schemeIdx + 3);
                }
            }
        }

        // Strip path/query fragment if present
        int slashIdx = host.indexOf('/');
        if (slashIdx >= 0) {
            host = host.substring(0, slashIdx);
        }

        // Trim again after extraction
        host = host.trim();
        if (host.isEmpty()) {
            return "Unable to extract host from input.";
        }

        // Handle IPv6 bracketed literal like [::1]:8080
        String hostForInet = host;
        if (hostForInet.startsWith("[") && hostForInet.contains("]")) {
            int endBracket = hostForInet.indexOf(']');
            hostForInet = hostForInet.substring(1, endBracket);
        } else {
            // Remove port portion for hostname: "example.com:8080" -> "example.com"
            int colonIdx = hostForInet.indexOf(':');
            if (colonIdx > 0 && hostForInet.indexOf(']') == -1) {
                // Only remove colon if it's not an IPv6 literal (IPv6 won't have brackets here)
                hostForInet = hostForInet.substring(0, colonIdx);
            }
        }

        if (hostForInet.isEmpty()) {
            return "Invalid host after processing input.";
        }

        // Basic validation: decide whether it looks like IPv6 or hostname/IPv4
        try {
            if (looksLikeIPv6(hostForInet)) {
                // Validate characters allowed in IPv6
                if (!Pattern.matches("^[0-9A-Fa-f:.]+$", hostForInet)) {
                    return "Invalid IPv6 address.";
                }
            } else {
                // Validate hostname (letters, digits, hyphen, dot) and label rules
                if (!isValidHostname(hostForInet)) {
                    return "Invalid hostname.";
                }
            }

            // Resolve and check reachability with a reasonable timeout.
            // Note: isReachable may require special privileges/ICMP support on some platforms
            InetAddress addr = InetAddress.getByName(hostForInet);
            final int timeoutMs = 4000; // 4 seconds
            boolean reachable;
            try {
                reachable = addr.isReachable(timeoutMs);
            } catch (SecurityException se) {
                return "Security manager prevented reachability check.";
            } catch (IOException ioe) {
                // Network I/O error while trying to reach host
                return "I/O error while checking host: " + ioe.getMessage();
            }

            StringBuilder sb = new StringBuilder(128);
            sb.append("Input: ").append(original).append(System.lineSeparator());
            sb.append("Resolved host: ").append(hostForInet).append(System.lineSeparator());
            sb.append("Address: ").append(addr.getHostAddress()).append(System.lineSeparator());
            sb.append("Reachable: ").append(reachable ? "yes" : "no").append(System.lineSeparator());
            return sb.toString();

        } catch (UnknownHostException uhe) {
            return "Unknown host: " + hostForInet;
        } catch (Exception e) {
            return "Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    private boolean looksLikeIPv6(String s) {
        return s.contains(":");
    }

    /**
     * Very conservative hostname validation:
     * - total length 1..253
     * - labels (separated by '.') are 1..63 characters
     * - labels contain only letters, digits, and hyphen, and do not start/end with hyphen
     */
    private boolean isValidHostname(String host) {
        if (host == null) return false;
        String h = host.trim();
        if (h.length() < 1 || h.length() > 253) return false;
        // Allow also IPv4 dotted decimal here; will be validated by InetAddress later
        if (Pattern.matches("^[0-9.]+$", h)) return true;

        String[] labels = h.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) return false;
            if (!Pattern.matches("^[A-Za-z0-9-]+$", label)) return false;
            if (label.startsWith("-") || label.endsWith("-")) return false;
        }
        return true;
    }
}
