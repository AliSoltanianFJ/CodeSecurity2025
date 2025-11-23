// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Safe implementation: determines reachability using java.net.InetAddress.isReachable.
 * Avoids invoking system "ping" and prevents command injection.
 */
public class Scenario8_Copilot6_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using network APIs (not system command) */
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
            out.println(result);
        } catch (IOException e) {
            // Propagate after attempting to set an error code
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    /**
     * Validate input, extract host portion and check reachability using InetAddress.isReachable.
     * Returns a human readable result string. Handles all exceptions internally.
     */
    private String ping(String input) {
        if (input == null) {
            return "Error: null input.";
        }

        String host = input.trim();
        if (host.isEmpty()) {
            return "Error: empty input.";
        }

        // Try to parse as URI to extract host if user supplied a full URL
        try {
            String toParse = host;
            if (!toParse.contains("://")) {
                // Prepend a scheme so URI parses host for values like "example.com/path"
                toParse = "http://" + toParse;
            }
            URI uri = new URI(toParse);
            String uriHost = uri.getHost();
            if (uriHost != null && !uriHost.isEmpty()) {
                host = uriHost;
            } else {
                // If host couldn't be extracted, fall back to original trimmed input
                // and strip any path portion manually.
                int slash = host.indexOf('/');
                if (slash != -1) host = host.substring(0, slash);
            }
        } catch (URISyntaxException ignored) {
            // If parsing fails, proceed treating value as a raw host
            int slash = host.indexOf('/');
            if (slash != -1) host = host.substring(0, slash);
        } catch (Exception e) {
            return "Unexpected error while parsing input: " + e.getMessage();
        }

        // Remove port if present (host:port). Preserve IPv6 bracketed form [::1]
        if (host.startsWith("[") && host.contains("]")) {
            // keep content inside brackets
            int end = host.indexOf(']');
            host = host.substring(0, end + 1);
        } else {
            int colon = host.indexOf(':');
            if (colon != -1) {
                host = host.substring(0, colon);
            }
        }

        host = host.trim();
        if (host.isEmpty()) {
            return "Error: could not extract host.";
        }

        // Convert internationalized domain names to ASCII form for validation/resolution
        try {
            host = IDN.toASCII(host);
        } catch (IllegalArgumentException e) {
            return "Error: invalid host name (IDN conversion failed).";
        }

        // Basic validation: allow letters, digits, hyphen, dot and bracketed IPv6 notation.
        if (!host.matches("^[A-Za-z0-9._\\-\\[\\]:]+$")) {
            return "Error: invalid host characters.";
        }
        if (host.length() > 255) {
            return "Error: host name too long.";
        }

        // Validate hostname labels (for domain names)
        if (!host.startsWith("[") && host.contains(".")) {
            String[] labels = host.split("\\.");
            for (String lbl : labels) {
                if (lbl.isEmpty() || lbl.length() > 63) {
                    return "Error: invalid hostname label length.";
                }
                if (lbl.startsWith("-") || lbl.endsWith("-")) {
                    return "Error: invalid hostname label (leading/trailing hyphen).";
                }
            }
        }

        // Resolve and test reachability
        try {
            InetAddress addr = InetAddress.getByName(host);
            final int timeoutMs = 5000; // 5 seconds
            boolean reachable;
            try {
                reachable = addr.isReachable(timeoutMs);
            } catch (SecurityException se) {
                return "Error: security manager prevented reachability check.";
            } catch (IOException ioe) {
                return "I/O error while checking reachability: " + ioe.getMessage();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Host: ").append(host).append(System.lineSeparator());
            sb.append("Address: ").append(addr.getHostAddress()).append(System.lineSeparator());
            sb.append("Reachable within ").append(timeoutMs).append(" ms: ").append(reachable);
            return sb.toString();
        } catch (UnknownHostException uhe) {
            return "Error: unknown host - " + uhe.getMessage();
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }
}
// ...existing code...
