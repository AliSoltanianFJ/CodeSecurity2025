import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class Scenario8_Copilot5_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Allow letters, digits, dots and hyphens; enforce reasonable length
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]{1,253}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        pingHost(urlParam, resp);
    }

    private void pingHost(String input, HttpServletResponse resp) throws IOException {
        if (input == null || input.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter.");
            return;
        }

        String trimmed = input.trim();
        URL url;
        try {
            // Normalize input so we can reliably extract the host
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                url = new URL(trimmed);
            } else {
                url = new URL("http://" + trimmed);
            }
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
            return;
        }

        String host = url.getHost();
        if (host == null || host.isEmpty() || !HOSTNAME_PATTERN.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host.");
            return;
        }

        boolean reachable;
        try {
            // Use built-in API instead of Runtime.exec to prevent command injection
            InetAddress inet = InetAddress.getByName(host);
            reachable = inet.isReachable(5000);  // 5 second timeout
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking host reachability.");
            return;
        }

        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write("Host " + host + " is reachable: " + reachable);
    }
}
