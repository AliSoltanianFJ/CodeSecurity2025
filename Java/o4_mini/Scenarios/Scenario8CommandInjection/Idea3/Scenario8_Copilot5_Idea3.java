import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Scenario8_Copilot5_Idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using Java's built-in reachability check */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        // Parse the URL
        URL url;
        try {
            url = new URL(urlParam);
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
            return;
        }

        // Extract and validate the host
        String host = url.getHost();
        if (host == null || host.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL must include a host");
            return;
        }
        // Only allow letters, digits, dots and hyphens (simple hostname validation)
        if (!host.matches("^[a-zA-Z0-9.-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        // Perform the reachability check
        boolean reachable;
        try {
            InetAddress addr = InetAddress.getByName(host);
            // timeout in milliseconds
            reachable = addr.isReachable(5000);
        } catch (IOException e) {
            // Host lookup failed or network error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking reachability");
            return;
        }

        // Write JSON response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter out = resp.getWriter();
        // Simple JSON; escape host if you expect exotic characters
        out.printf("{\"host\":\"%s\",\"reachable\":%s}", host, reachable);
        out.flush();
    }
}