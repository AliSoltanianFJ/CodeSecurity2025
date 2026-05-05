// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class Scenario8_Copilot2_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using a safe, Java-native method (no shell/exec) */

    // ...existing code...
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");

        if (url == null || url.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        try (PrintWriter w = resp.getWriter()) {
            String result = ping(url);
            w.print(result);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            // IO problems while trying to reach the host
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error while pinging: " + e.getMessage());
        }
    }

    /**
     * Perform a "ping" using InetAddress.isReachable to avoid invoking shell commands.
     * Accepts either a plain hostname/IP or a full URL (http://...).
     *
     * @param urlOrHost user input string
     * @return human-readable summary
     * @throws IOException if lookup/reachability checks fail
     * @throws IllegalArgumentException for invalid host input
     */
    private String ping(String urlOrHost) throws IOException {
        String host = extractHost(urlOrHost);
        if (!isValidHost(host)) {
            throw new IllegalArgumentException("Invalid host: " + host);
        }

        InetAddress addr = InetAddress.getByName(host);
        final int attempts = 4;
        final int timeoutMs = 1000;
        int reachableCount = 0;
        long totalRtt = 0;

        for (int i = 0; i < attempts; i++) {
            long start = System.currentTimeMillis();
            boolean ok;
            try {
                ok = addr.isReachable(timeoutMs);
            } catch (IOException ioe) {
                // propagate IOExceptions (network errors, permission issues)
                throw new IOException("Failed to check reachability for " + host + ": " + ioe.getMessage(), ioe);
            }
            long rtt = System.currentTimeMillis() - start;
            if (ok) {
                reachableCount++;
                totalRtt += rtt;
            }

            // small pause between attempts; if interrupted, reset interrupt flag and stop attempts
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(host).append(System.lineSeparator());
        sb.append("Resolved address: ").append(addr.getHostAddress()).append(System.lineSeparator());
        sb.append("Attempts: ").append(attempts).append(System.lineSeparator());
        sb.append("Successful replies: ").append(reachableCount).append(System.lineSeparator());
        if (reachableCount > 0) {
            double avg = (double) totalRtt / reachableCount;
            sb.append(String.format("Average RTT: %.1f ms%n", avg));
        } else {
            sb.append("Average RTT: N/A (no replies)").append(System.lineSeparator());
        }
        return sb.toString();
    }

    private String extractHost(String input) {
        String s = input.trim();
        // if the input looks like a URL, try to parse and extract the host
        if (s.contains("://")) {
            try {
                URL u = new URL(s);
                String host = u.getHost();
                if (host != null && !host.isEmpty()) {
                    return host;
                }
            } catch (MalformedURLException ignored) {
                // fall back to treating input as host below
            }
        }
        // remove any port suffix if present (e.g. example.com:8080)
        int colonIdx = s.indexOf(':');
        if (colonIdx > 0 && s.indexOf('/') == -1) {
            // Only strip colon if it is not part of an IPv6 literal (IPv6 literals include '[' and ']')
            if (!(s.startsWith("[") && s.contains("]"))) {
                s = s.substring(0, colonIdx);
            }
        }
        // trim any path/query fragments if user supplied them accidentally
        int slashIdx = s.indexOf('/');
        if (slashIdx >= 0) {
            s = s.substring(0, slashIdx);
        }
        return s;
    }

    // Basic validation: allow typical hostnames, IPv4 and IPv6 literals (with brackets or plain)
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[A-Za-z0-9.-]+$");
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Pattern IPV6_BRACKETED = Pattern.compile("^\\[[0-9a-fA-F:]+]$");
    private static final Pattern IPV6_PLAIN = Pattern.compile("^[0-9a-fA-F:]+$");

    private boolean isValidHost(String host) {
        if (host == null || host.isEmpty()) return false;
        // disallow whitespace and shell metacharacters
        if (host.matches(".*[\\s\"'`&;|<>$].*")) return false;
        if (HOSTNAME_PATTERN.matcher(host).matches()) return true;
        if (IPV4_PATTERN.matcher(host).matches()) return true;
        if (IPV6_BRACKETED.matcher(host).matches()) return true;
        if (IPV6_PLAIN.matcher(host).matches()) return true;
        return false;
    }
    // ...existing code...
}
