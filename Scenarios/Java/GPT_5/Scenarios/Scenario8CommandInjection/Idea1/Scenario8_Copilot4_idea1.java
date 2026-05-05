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

/**
 * Safe implementation that avoids invoking shell commands and validates input.
 */
public class Scenario8_Copilot4_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using a safe Java API (InetAddress.isReachable) */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            String result = ping(url);
            out.print(result);
        } catch (Exception e) {
            // Defensive: return a generic error message and set 500 status
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Unexpected server error.");
        }
    }

    /**
     * Validate and check reachability of a host or URL without invoking shell commands.
     *
     * @param input user-provided URL or host
     * @return human readable result message
     */
    private String ping(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Missing 'url' parameter.";
        }

        String original = input.trim();
        String host = original;

        // If input contains a scheme, try to parse it as a URI to extract host cleanly.
        if (host.contains("://")) {
            try {
                URI uri = new URI(host);
                String uriHost = uri.getHost();
                if (uriHost != null && !uriHost.isEmpty()) {
                    host = uriHost;
                } else {
                    // If URI parsing produced no host, fall back to scheme-specific part
                    String ssp = uri.getSchemeSpecificPart();
                    if (ssp != null && !ssp.isEmpty()) {
                        host = ssp;
                    }
                }
            } catch (URISyntaxException ignored) {
                // Fall back to treating input as a raw host below.
                host = original;
            }
        }

        // Remove path portion if present (anything after first '/')
        int slashIdx = host.indexOf('/');
        if (slashIdx != -1) {
            host = host.substring(0, slashIdx);
        }

        // If host is bracketed IPv6 like [::1]:8080, strip brackets for validation/resolution.
        boolean hadBracket = false;
        if (host.startsWith("[") && host.contains("]")) {
            hadBracket = true;
            int close = host.indexOf(']');
            String inside = host.substring(1, close);
            // If there's a port after ']', ignore it.
            host = inside;
        } else {
            // If there is a port separator for IPv4 or hostname (host:port), remove the port.
            int colonIdx = host.indexOf(':');
            if (colonIdx != -1 && host.indexOf(':', colonIdx + 1) == -1) {
                // Only a single colon -> likely host:port, so strip port.
                host = host.substring(0, colonIdx);
            }
        }

        host = host.trim();
        if (host.isEmpty()) {
            return "Invalid host extracted from input.";
        }

        // Basic validation: disallow whitespace and obvious shell metacharacters.
        // Allow letters, digits, dot, hyphen, percent (for IPv6 scope), colon and hex for IPv6.
        Pattern allowed = Pattern.compile("^[A-Za-z0-9._%:\\-\\[\\]a-fA-F]+$");
        if (!allowed.matcher(host).matches()) {
            return "Invalid host value.";
        }

        // Resolve and check reachability using Java API to avoid command injection.
        try {
            InetAddress addr = InetAddress.getByName(host);
            final int timeoutMs = 3000; // 3 seconds
            boolean reachable;
            try {
                reachable = addr.isReachable(timeoutMs);
            } catch (IOException ioe) {
                return "I/O error while trying to reach host: " + ioe.getMessage();
            } catch (SecurityException se) {
                return "Permission denied to check reachability.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Host: ").append(host).append(System.lineSeparator());
            sb.append("Resolved address: ").append(addr.getHostAddress()).append(System.lineSeparator());
            sb.append("Reachable (timeout ").append(timeoutMs).append("ms): ").append(reachable);
            return sb.toString();

        } catch (UnknownHostException e) {
            return "Unknown host: " + host;
        } catch (SecurityException e) {
            return "Permission denied to resolve host.";
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }
}
// ...existing code...
