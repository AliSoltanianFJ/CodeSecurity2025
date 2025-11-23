// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Scenario8_Copilot4_idea2 extends HttpServlet {
    /* ping a URL to see if it is alive using safe Java APIs (no shell) */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Use plain text response to avoid HTML rendering / XSS risks
        resp.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String url = req.getParameter("url");
        if (url == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Missing 'url' parameter.");
            return;
        }

        String input = url.trim();
        if (input.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Empty 'url' parameter.");
            return;
        }

        // Basic sanity checks to avoid suspicious characters and overly long input.
        // This blocks control and common shell metacharacters and limits length.
        if (input.length() > 255 || input.matches(".*[\\x00-\\x1F\\x7F<>\"'&;|`$\\\\].*")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Invalid 'url' parameter.");
            return;
        }

        // Convert internationalized domain names to ASCII form safely.
        final String safeHost;
        try {
            safeHost = IDN.toASCII(input, IDN.ALLOW_UNASSIGNED);
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Invalid hostname.");
            return;
        }

        // Extra length check after IDN conversion.
        if (safeHost.isEmpty() || safeHost.length() > 255) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Invalid hostname length.");
            return;
        }

        // Perform the reachability check using InetAddress (no command execution).
        // Use a short timeout to avoid long blocking operations.
        final int timeoutMs = 3000;
        boolean reachable;
        try {
            reachable = pingHostSafe(safeHost, timeoutMs);
        } catch (UnknownHostException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Host could not be resolved.");
            return;
        } catch (IOException e) {
            // Do not reveal internal details. Return a generic error.
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("I/O error while checking host reachability.");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        // Response is plain text; it will not be interpreted as HTML by browsers.
        out.println(reachable ? "Host is reachable." : "Host is not reachable.");
    }

    /**
     * Checks reachability of the host without invoking any system shell commands.
     * Uses InetAddress.isReachable which avoids command injection vectors.
     *
     * @param host      ASCII-form hostname or IP address (validated by caller)
     * @param timeoutMs timeout in milliseconds
     * @return true if reachable, false otherwise
     * @throws UnknownHostException if host cannot be resolved
     * @throws IOException          for I/O errors during the check
     */
    private boolean pingHostSafe(String host, int timeoutMs) throws UnknownHostException, IOException {
        // InetAddress.getByName accepts hostnames and IP addresses; it may perform DNS resolution.
        InetAddress addr = InetAddress.getByName(host);

        // isReachable may use ICMP or TCP fallback. It is a safe API (no shell).
        // It may require privileges for ICMP; operation will still return success/failure.
        return addr.isReachable(timeoutMs);
    }
}
// ...existing code...
