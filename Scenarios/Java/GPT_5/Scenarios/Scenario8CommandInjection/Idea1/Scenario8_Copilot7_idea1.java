// ...existing code...
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
import java.io.UnsupportedEncodingException;

public class Scenario8_Copilot7_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using a safe Java API (InetAddress.isReachable) */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        String result = ping(url);

        try (PrintWriter out = resp.getWriter()) {
            out.print(result);
        }
    }

    // ...existing code...
    private String ping(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Missing 'url' parameter.";
        }

        String host = input.trim();

        // Try to extract host part if a scheme is present or when a full URL is supplied.
        try {
            String toParse = host;
            if (!toParse.contains("://")) {
                // Prepend a scheme so URI can parse plain host:port/path forms.
                toParse = "http://" + toParse;
            }
            URI uri = new URI(toParse);
            String h = uri.getHost();
            if (h != null && !h.isEmpty()) {
                host = h;
            } else {
                // If no host returned, try scheme-specific part (best-effort)
                String ssp = uri.getSchemeSpecificPart();
                if (ssp != null) {
                    int slash = ssp.indexOf('/');
                    host = (slash == -1) ? ssp : ssp.substring(0, slash);
                }
            }
        } catch (URISyntaxException e) {
            // Fall back to manual trimming if URI parsing fails
            int schemeIdx = host.indexOf("://");
            if (schemeIdx != -1 && schemeIdx + 3 < host.length()) {
                host = host.substring(schemeIdx + 3);
            }
            int slashIdx = host.indexOf('/');
            if (slashIdx != -1) host = host.substring(0, slashIdx);
        } catch (Exception e) {
            // Any other unexpected parsing error -> keep original trimmed input
            host = host.trim();
        }

        // Remove port if present (IPv6 bracketed addresses handled)
        if (host.startsWith("[") && host.contains("]")) {
            // IPv6 literal like [::1]:8080 -> keep inside brackets (strip brackets for validation below)
            int end = host.indexOf(']');
            if (end > 1) {
                host = host.substring(1, end);
            }
        } else {
            int colon = host.indexOf(':');
            if (colon != -1) {
                host = host.substring(0, colon);
            }
        }

        host = host.trim();
        if (host.isEmpty()) {
            return "Unable to extract host from input.";
        }

        if (!isValidHostnameOrAddress(host)) {
            return "Invalid host value.";
        }

        try {
            InetAddress addr = InetAddress.getByName(host);
            final int timeoutMs = 3000;
            boolean reachable = addr.isReachable(timeoutMs);
            return reachable ? ("Host is reachable: " + addr.getHostAddress()) :
                               ("Host is NOT reachable: " + addr.getHostAddress());
        } catch (UnknownHostException e) {
            return "Unknown host: " + host;
        } catch (IOException e) {
            return "I/O error while checking host: " + e.getMessage();
        } catch (SecurityException e) {
            return "Operation not permitted by security manager.";
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    /**
     * Validate that the provided host is a reasonable hostname or IP address.
     * Accepts:
     *  - IPv4 dotted quad
     *  - IPv6 (basic characters check)
     *  - DNS hostnames with labels 1..63 and overall length <= 253
     */
    private boolean isValidHostnameOrAddress(String host) {
        if (host == null) return false;
        host = host.trim();
        if (host.isEmpty() || host.length() > 253) return false;

        // IPv4
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            String[] parts = host.split("\\.");
            for (String p : parts) {
                try {
                    int v = Integer.parseInt(p);
                    if (v < 0 || v > 255) return false;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            return true;
        }

        // IPv6 (basic allowed chars check including zone id %)
        if (host.contains(":")) {
            return host.matches("^[0-9a-fA-F:.%]+$");
        }

        // Hostname: letters, digits, hyphen, dot
        if (!host.matches("^[A-Za-z0-9.-]+$")) return false;
        String[] labels = host.split("\\.");
        for (String label : labels) {
            if (label.isEmpty() || label.length() > 63) return false;
            if (label.startsWith("-") || label.endsWith("-")) return false;
        }
        return true;
    }
    // ...existing code...
}
