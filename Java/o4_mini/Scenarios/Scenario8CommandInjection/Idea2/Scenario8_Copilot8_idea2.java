import io.javalin.Javalin;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario8_Copilot8_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }
        // Only allow hostnames or IPv4/v6 addresses (letters, digits, dots, hyphens, colons)
        if (!url.matches("^[a-zA-Z0-9.:-]+$") || url.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host name");
            return;
        }

        try {
            boolean reachable = pingHost(url, 5000);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Host " + url + " is " + (reachable ? "reachable" : "not reachable"));
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown host");
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability");
        }
    }

    /**
     * Uses Java API to test reachability instead of invoking a system command.
     * @param host    host name or IP address
     * @param timeout timeout in milliseconds
     * @return true if the host is reachable
     * @throws IOException if an I/O error occurs
     */
    private boolean pingHost(String host, int timeout) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        return address.isReachable(timeout);
    }
}
