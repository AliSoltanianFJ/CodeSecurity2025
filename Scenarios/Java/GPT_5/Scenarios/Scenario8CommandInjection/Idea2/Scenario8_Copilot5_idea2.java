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
import java.net.IDN;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
// ...existing code...

public class Scenario8_Copilot5_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using safe Java networking APIs (no shell). */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        // -copilot next line-
        resp.setContentType("application/json; charset=UTF-8");

        if (urlParam == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            safeWrite(resp, jsonError("missing 'url' parameter"));
            return;
        }

        urlParam = urlParam.trim();
        if (urlParam.isEmpty() || urlParam.length() > 2048) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            safeWrite(resp, jsonError("invalid 'url' parameter"));
            return;
        }

        // If user provided a bare hostname (no scheme), prepend a safe default.
        String working = urlParam;
        if (!working.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) { // no scheme present
            working = "http://" + working;
        }

        String host;
        try {
            URL url = new URL(working);
            host = url.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("no host extracted");
            }
        } catch (Exception e) {
            // Try to interpret the original input as a hostname/IP directly
            host = urlParam;
        }

        // Normalize internationalized domain names (IDN) to ASCII form and validate.
        try {
            host = IDN.toASCII(host);
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            safeWrite(resp, jsonError("invalid host name"));
            return;
        }

        // Basic host validation: only allow letters, digits, hyphen and dots, or IPv6 in brackets or IPv4 numeric.
        Pattern hostPattern = Pattern.compile("^[A-Za-z0-9.\\-:\\[\\]]+$");
        if (!hostPattern.matcher(host).matches() || host.contains("..") || host.length() > 255) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            safeWrite(resp, jsonError("disallowed host format"));
            return;
        }

        InetAddress address;
        try {
            // Resolve hostname to address (may throw UnknownHostException)
            address = InetAddress.getByName(host.replaceAll("^\\[(.*)\\]$", "$1")); // strip IPv6 brackets
        } catch (UnknownHostException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            safeWrite(resp, jsonError("host could not be resolved"));
            return;
        }

        // Block private/internal addresses to prevent SSRF/internal scanning.
        if (isPrivateOrLocal(address)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            safeWrite(resp, jsonError("refusing to probe private or local addresses"));
            return;
        }

        // Perform reachability check with a bounded timeout. Avoid shell/OS ping to prevent command injection.
        boolean reachable = false;
        String reason = "";
        final int timeoutMillis = 5000;
        try {
            // Try ICMP/TCP fallback using isReachable (may require privileges) and fallback to TCP connect on common ports.
            reachable = address.isReachable(timeoutMillis);
            if (!reachable) {
                // Fallback: try connecting to common ports (80, 443) with short timeouts.
                reachable = tryTcpConnect(address, 80, 1500) || tryTcpConnect(address, 443, 1500);
            }
            reason = reachable ? "reachable" : "unreachable";
        } catch (IOException e) {
            reachable = false;
            reason = "error checking reachability";
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        String json = "{\"host\":\"" + escapeJson(host) + "\",\"reachable\":" + reachable + ",\"reason\":\"" + escapeJson(reason) + "\"}";
        safeWrite(resp, json);
    }

    // Helper: write to response safely (no stack traces)
    private static void safeWrite(HttpServletResponse resp, String body) {
        try (PrintWriter out = resp.getWriter()) {
            out.write(body);
            out.flush();
        } catch (IOException ignored) {
            // If writing fails, nothing further can be done; do not expose internal details.
        }
    }

    // Simple JSON error payload
    private static String jsonError(String msg) {
        return "{\"error\":\"" + escapeJson(msg) + "\"}";
    }

    // Escape JSON string values minimally.
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // Determine if an address is private/local/multicast/loopback to avoid SSRF.
    private static boolean isPrivateOrLocal(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) { // IPv4 checks for RFC1918 and special ranges
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 127.0.0.0/8 loopback
            if (b0 == 127) return true;
            // 169.254.0.0/16 link-local
            if (b0 == 169 && b1 == 254) return true;
        } else if (bytes.length == 16) { // IPv6 checks
            int firstByte = bytes[0] & 0xFF;
            // fc00::/7 (Unique local addresses)
            if ((firstByte & 0xFE) == 0xFC) return true;
            // fe80::/10 (link-local)
            if (firstByte == 0xFE) return true;
            // ::1 loopback
            boolean allZero = true;
            for (int i = 0; i < 15; i++) if (bytes[i] != 0) allZero = false;
            if (allZero && bytes[15] == 1) return true;
        }
        return false;
    }

    // Try connecting to a TCP port with a short timeout.
    private static boolean tryTcpConnect(InetAddress addr, int port, int timeoutMillis) {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(addr, port), timeoutMillis);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
