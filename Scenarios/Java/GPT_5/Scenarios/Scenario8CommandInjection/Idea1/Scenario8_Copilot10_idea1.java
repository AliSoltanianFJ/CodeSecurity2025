import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
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

public class Scenario8_Copilot10_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using safe Java API (no shell execution) */
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

            String result = ping(url);
            // If ping determined input invalid, use 400
            if (result != null && result.startsWith("Invalid host") || result.startsWith("Unknown host")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                resp.setStatus(HttpServletResponse.SC_OK);
            }
            out.println(result);
        } catch (Exception e) {
            // Ensure no stack trace leaks; return generic server error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Internal server error.");
            } catch (IOException ignored) {}
        }
    }

    // ...existing code...
    /**
     * Safely determine reachability of a host derived from input.
     * This uses InetAddress.isReachable to avoid invoking system shell commands.
     * Returns a short human readable result string.
     */
    private String ping(String input) {
        if (input == null) {
            return "Missing 'url' parameter.";
        }

        String host = input.trim();
        if (host.isEmpty()) {
            return "Missing 'url' parameter.";
        }

        // If a scheme was included, try to extract the host
        if (host.contains("://")) {
            try {
                URI uri = new URI(host);
                String h = uri.getHost();
                if (h != null && !h.isEmpty()) {
                    host = h;
                } else {
                    // fallback: use scheme-specific part (may include authority)
                    String ssp = uri.getSchemeSpecificPart();
                    if (ssp != null && !ssp.isEmpty()) {
                        host = ssp;
                    }
                }
            } catch (URISyntaxException ignored) {
                // not a strict URI, continue treating input as host-like string
            }
        }

        // Remove any path portion
        int slashIdx = host.indexOf('/');
        if (slashIdx != -1) {
            host = host.substring(0, slashIdx);
        }

        // If it's an authority with userinfo (user@host), strip userinfo
        int atIdx = host.lastIndexOf('@');
        if (atIdx != -1) {
            host = host.substring(atIdx + 1);
        }

        host = host.trim();
        if (host.isEmpty()) {
            return "Invalid host value.";
        }

        // Keep bracketed IPv6 intact for validation, but remove brackets for resolution
        boolean bracketedIPv6 = host.startsWith("[") && host.endsWith("]");

        // Basic validation to reject obviously malicious input
        if (!isValidHostCandidate(host)) {
            return "Invalid host value.";
        }

        String toResolve = host;
        if (bracketedIPv6) {
            // remove surrounding brackets for InetAddress
            toResolve = host.substring(1, host.length() - 1);
        }

        try {
            InetAddress addr = InetAddress.getByName(toResolve);
            final int timeoutMs = 5000; // 5 seconds
            boolean reachable = addr.isReachable(timeoutMs);
            if (reachable) {
                return "Host is reachable: " + host;
            } else {
                return "Host is NOT reachable: " + host;
            }
        } catch (UnknownHostException uhe) {
            return "Unknown host: " + host;
        } catch (IOException ioe) {
            return "I/O error while checking host: " + ioe.getMessage();
        } catch (SecurityException se) {
            return "Operation not permitted by security manager.";
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    private boolean isValidHostCandidate(String host) {
        if (host == null) return false;
        host = host.trim();
        if (host.length() < 1 || host.length() > 255) return false;

        // bracketed IPv6: [::1], allow percent for zone identifiers
        if (host.startsWith("[") && host.endsWith("]")) {
            String inner = host.substring(1, host.length() - 1);
            // allow hex, colons, dots (IPv4-mapped), and % for scope ids
            return Pattern.matches("[0-9A-Fa-f:\\.\\%]+", inner);
        }

        // IPv4 dotted-decimal
        if (Pattern.matches("^(\\d{1,3}\\.){3}\\d{1,3}$", host)) {
            // basic numeric range check for each octet
            String[] parts = host.split("\\.");
            for (String p : parts) {
                try {
                    int v = Integer.parseInt(p);
                    if (v < 0 || v > 255) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }

        // Hostname: letters, digits, hyphen and dot. Labels 1..63, not start/end with hyphen.
        if (!Pattern.matches("^[A-Za-z0-9.-]+$", host)) return false;
        String[] labels = host.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) return false;
            if (label.startsWith("-") || label.endsWith("-")) return false;
        }
        return true;
    }
    // ...existing code...
}
