// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.IDN;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

public class Scenario8_Copilot3_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.isBlank() || urlParam.length() > 2048) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        URL url;
        try {
            url = new URL(urlParam);
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        String protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        String host = url.getHost();
        if (host == null || host.isBlank() || host.length() > 255) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        String asciiHost;
        try {
            asciiHost = IDN.toASCII(host);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        int port = url.getPort();
        if (port == -1) {
            port = "https".equalsIgnoreCase(protocol) ? 443 : 80;
        }
        boolean reachable;
        int timeoutMs = 3000;

        try (Socket socket = new Socket()) {
            InetAddress addr = InetAddress.getByName(asciiHost);
            socket.connect(new InetSocketAddress(addr, port), timeoutMs);
            reachable = socket.isConnected();
        } catch (IOException e) {
            reachable = false;
        }

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter out = resp.getWriter()) {
            out.print("{\"reachable\":");
            out.print(reachable ? "true" : "false");
            out.print("}");
        }
    }
}
// ...existing code...
