// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Secure implementation: no shell invocation. Uses InetAddress.isReachable and
 * conservative input parsing/validation to avoid command injection risks.
 */
public class Scenario8_Copilot2_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        String result = ping(url);

        try (PrintWriter out = resp.getWriter()) {
            out.println(result);
        } catch (IOException e) {
            // If writing the response fails, ensure we propagate an appropriate error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to write response");
        }
    }

    /**
     * Determine reachability of a host derived from the user-provided input.
     * This method avoids launching system commands and performs careful parsing
     * and validation of the input.
     *
     * @param input user-supplied URL or hostname
     * @return human readable result message
     */
    private String ping(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Missing 'url' parameter.";
        }

        String host = input.trim();

        try {
            // If input looks like a URI, try to extract the host portion.
            if (host.contains("://")) {
                try {
                    URI uri = new URI(host);
                    if (uri.getHost() != null && !uri.getHost().isEmpty()) {
                        host = uri.getHost();
                    } else {
                        // Fallback to URL parsing which may handle some cases differently
                        URL url = new URL(host);
                        if (url.getHost() != null && !url.getHost().isEmpty()) {
                            host = url.getHost();
                        }
                    }
                } catch (Exception ignored) {
                    // If parsing fails, strip the scheme part and continue
                    int idx = host.indexOf("://");
                    if (idx != -1 && idx + 3 < host.length()) {
                        host = host.substring(idx + 3);
                    }
                }
            }

            // Remove path/query/fragment if present
            int slashIdx = host.indexOf('/');
            if (slashIdx != -1) {
                host = host.substring(0, slashIdx);
            }

            // Handle IPv6 zone/bracketed notation: [::1]:port or [::1]
            if (host.startsWith("[") && host.contains("]")) {
                int endBracket = host.indexOf(']');
                if (endBracket > 1) {
                    host = host.substring(1, endBracket); // inside brackets
                } else {
                    return "Invalid IPv6 host format.";
                }
            } else {
                // Remove single port suffix if present (hostname:port). If multiple ':' present it's likely IPv6; keep it.
                int firstColon = host.indexOf(':');
                if (firstColon != -1 && host.indexOf(':', firstColon + 1) == -1) {
                    host = host.substring(0, firstColon);
                }
            }

            host = host.trim();
            if (host.isEmpty()) {
                return "Unable to extract host from input.";
            }

            // Basic validation: allow letters, digits, dot, hyphen, and colon for IPv6 hex. Reject other characters.
            Pattern allowed = Pattern.compile("^[A-Za-z0-9.:-]+$");
            if (!allowed.matcher(host).matches()) {
                return "Invalid characters in host.";
            }

            // Length checks
            if (host.length() > 253) {
                return "Host name is too long.";
            }

            // Resolve the host to ensure it is valid
            InetAddress addr;
            try {
                addr = InetAddress.getByName(host);
            } catch (UnknownHostException uhe) {
                return "Unknown host: " + host;
            }

            // Timeout in milliseconds for reachability test
            final int timeoutMs = 5000;
            boolean reachable;
            try {
                reachable = addr.isReachable(timeoutMs);
            } catch (IllegalArgumentException iae) {
                return "Invalid address: " + iae.getMessage();
            } catch (SecurityException se) {
                return "Operation not permitted by security manager.";
            } catch (IOException ioe) {
                return "I/O error while testing reachability: " + ioe.getMessage();
            }

            return reachable ? "Host is reachable: " + host : "Host is NOT reachable: " + host;

        } catch (Exception e) {
            // Catch-all to ensure compilation and robust behavior
            return "Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}
// ...existing code...
