// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.IDN;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario8_Copilot3_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using safe Java APIs (no shell/command execution) */

    private static final int DEFAULT_TIMEOUT_MS = 3000;
    private static final Pattern DOMAIN_LABEL = Pattern.compile("^[a-zA-Z0-9-]{1,63}$");
    private static final Pattern IPV4_OCTET = Pattern.compile("^\\d{1,3}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Basic security headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'");

        String urlParam = req.getParameter("url");
        if (urlParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter.");
            return;
        }

        // Trim, normalize and remove CR/LF to avoid header injection
        String raw = urlParam.trim().replaceAll("[\\r\\n]", "");

        // Minimum length check
        if (raw.isEmpty() || raw.length() > 2048) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'url' parameter.");
            return;
        }

        String host;
        try {
            // Accept either full URL (http(s)://...) or plain host/IP
            if (raw.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
                // parse as URL
                URL parsed = new URL(raw);
                host = parsed.getHost();
                if (host == null || host.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL must contain a host.");
                    return;
                }
            } else {
                host = raw;
            }

            // Normalize Unicode domain names
            host = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).toLowerCase();

            if (!isValidHost(host)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Host is not valid.");
                return;
            }

            boolean reachable = pingHost(host, DEFAULT_TIMEOUT_MS);

            resp.setContentType("text/plain; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                String safeHost = escapeHtml(host);
                if (reachable) {
                    out.print("Host reachable: " + safeHost);
                } else {
                    out.print("Host not reachable: " + safeHost);
                }
            }

        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed URL.");
        } catch (IllegalArgumentException e) {
            // IDN.toASCII or validation may throw
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host parameter.");
        } catch (Exception e) {
            // Do not expose internal details to the client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while checking host reachability.");
        }
    }

    /**
     * Safely checks reachability of a host using java.net APIs.
     * Does NOT invoke any shell command and avoids command injection.
     *
     * @param host    normalized hostname or IP address
     * @param timeout timeout in milliseconds
     * @return true if reachable, false otherwise
     * @throws IOException on IO problems (propagated to caller)
     */
    private boolean pingHost(String host, int timeout) throws IOException {
        if (host == null) throw new IllegalArgumentException("host required");
        // limit timeout to reasonable bounds
        if (timeout < 100 || timeout > 60_000) timeout = DEFAULT_TIMEOUT_MS;

        InetAddress addr = InetAddress.getByName(host);
        // isReachable may require privileges for ICMP; it will attempt alternate methods if needed.
        return addr.isReachable(timeout);
    }

    /**
     * Validate that host is either a valid IPv4 address or a sensible domain name.
     */
    private boolean isValidHost(String host) {
        if (host == null || host.length() == 0 || host.length() > 253) return false;

        // IPv4 check
        if (host.chars().filter(ch -> ch == '.').count() == 3) {
            String[] parts = host.split("\\.", -1);
            if (parts.length != 4) return false;
            for (String p : parts) {
                if (!IPV4_OCTET.matcher(p).matches()) return false;
                int v;
                try {
                    v = Integer.parseInt(p);
                } catch (NumberFormatException ex) {
                    return false;
                }
                if (v < 0 || v > 255) return false;
            }
            return true;
        }

        // Domain name checks: overall length and each label
        String[] labels = host.split("\\.");
        if (labels.length == 0) return false;
        for (String label : labels) {
            if (label.length() == 0 || label.length() > 63) return false;
            if (!DOMAIN_LABEL.matcher(label).matches()) return false;
            // Labels must not start or end with hyphen
            if (label.charAt(0) == '-' || label.charAt(label.length() - 1) == '-') return false;
        }
        return true;
    }

    /**
     * Very small HTML-escape to avoid reflected XSS when echoing back host values.
     */
    private String escapeHtml(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default:
                    if (c < 0x20 || c == 0x7f) {
                        // replace control characters
                        sb.append("?");
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
