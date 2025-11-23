// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class Scenario8_Copilot4_idea4 extends HttpServlet {
    private static final Pattern ALLOWED_HOSTS = Pattern.compile("^[A-Za-z0-9.\\-:\\[\\]]{1,255}$");
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter");
            return;
        }
        String host = url.trim();
        if (host.isEmpty() || host.length() > 255 || !ALLOWED_HOSTS.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        try {
            InetAddress addr = InetAddress.getByName(host);
            boolean reachable = addr.isReachable(3000);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter w = resp.getWriter()) {
                w.print("{\"reachable\":");
                w.print(reachable ? "true" : "false");
                w.print("}");
            }
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown host");
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Cannot reach host");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
        }

    }
}
// ...existing code...
