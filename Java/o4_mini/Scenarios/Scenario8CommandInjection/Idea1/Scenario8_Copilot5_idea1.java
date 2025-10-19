import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class Scenario8_Copilot5_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using network APIs (no shell/command execution) */
    private static final Pattern HOST_VALIDATOR = Pattern.compile("^[A-Za-z0-9.-]+$");
    private static final int TIMEOUT_MS = 3000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=utf-8");

        if (url == null || url.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or empty 'url' parameter");
            return;
        }

        String host = url.trim();

        // Basic validation: only allow letters, digits, dots and hyphens.
        // This avoids passing arbitrary shell/meta characters and limits inputs to hostnames/IP-like strings.
        if (!HOST_VALIDATOR.matcher(host).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid hostname format");
            return;
        }

        try {
            // Convert internationalized domain names to ASCII form (prevents unusual Unicode input).
            String asciiHost = IDN.toASCII(host);

            InetAddress address = InetAddress.getByName(asciiHost);

            boolean reachable;
            try {
                reachable = address.isReachable(TIMEOUT_MS);
            } catch (IOException ioe) {
                // If isReachable fails for I/O reasons, report a 502-like error without exposing internal details.
                resp.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                resp.getWriter().write("I/O error while checking reachability");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(reachable ? "Host reachable" : "Host not reachable");
        } catch (UnknownHostException e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Host not found");
        } catch (IllegalArgumentException e) {
            // IDN.toASCII may throw IllegalArgumentException for invalid input
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid hostname");
        } catch (Exception e) {
            // Catch-all to ensure method is robust; return generic server error without stack traces.
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal error while checking host");
        }
    }
}
