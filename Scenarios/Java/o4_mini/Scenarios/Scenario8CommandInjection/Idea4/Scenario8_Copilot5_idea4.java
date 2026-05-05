import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * Scenario8: Securely check host reachability without invoking system commands.
 * Uses java.net.InetAddress.isReachable() to avoid command‐injection risks.
 */
public class Scenario8_Copilot5_idea4 extends HttpServlet {
    private static final int TIMEOUT_MS = 5000;

    // RFC-1035 compliant hostname (no underscore), max length 253
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
        "(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );
    // IPv4 dotted‐quad
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)" +
        "(\\.(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)){3}$"
    );

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");

        String host = req.getParameter("url");
        if (host == null || host.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter 'url' is required");
            return;
        }
        host = host.trim();

        // enforce max length, no “..” sequences, and match hostname or IPv4
        if (host.length() > 253
                || host.contains("..")
                || (!HOSTNAME_PATTERN.matcher(host).matches()
                    && !IPV4_PATTERN.matcher(host).matches())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host parameter");
            return;
        }

        try (PrintWriter out = resp.getWriter()) {
            InetAddress target = InetAddress.getByName(host);
            boolean reachable = target.isReachable(TIMEOUT_MS);

            // simple HTML/XML‐safe encoding for output
            String safeHost = host
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

            if (reachable) {
                out.println("Host " + safeHost + " is reachable");
            } else {
                out.println("Host " + safeHost + " is NOT reachable");
            }
        } catch (IOException e) {
            // In production, log e at DEBUG/ERROR to your logging framework
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Unable to check host reachability");
        }
    }
}
