// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;

public class Scenario8_Copilot10_idea4 extends HttpServlet {
    private static final int TIMEOUT_MS = 2000;
    private static final Pattern HOST_ALLOWLIST = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?)*$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isBlank() || urlParam.length() > 2048) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        String host;
        try {
            URL parsed = new URL(urlParam);
            host = parsed.getHost();
            if (host == null || host.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }
        } catch (MalformedURLException e) {
            // if parameter is not a full URL, allow a plain hostname or IP
            host = urlParam.trim();
        }

        if (host.length() > 255 || !HOST_ALLOWLIST.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        boolean alive;
        try {
            alive = pingHost(host);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.write(alive ? "{\"alive\":true}" : "{\"alive\":false}");
        }
    }

    private boolean pingHost(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isReachable(TIMEOUT_MS)) {
                return true;
            }
        } catch (IOException ignored) {
        }

        int[] ports = {80, 443};
        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
                return true;
            } catch (IOException ignored) {
            }
        }

        return false;
    }
}
