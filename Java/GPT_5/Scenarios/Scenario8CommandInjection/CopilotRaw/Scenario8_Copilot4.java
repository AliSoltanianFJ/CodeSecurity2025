// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;

public class Scenario8_Copilot4 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // ...existing code...
        if (url == null || url.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing 'url' parameter");
            return;
        }

        String host = url.trim();

        if (!isValidHost(host)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid host name or IP address");
            return;
        }

        try {
            String result = pingHost(host);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Ping was interrupted");
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("I/O error while executing ping: " + e.getMessage());
        }
    }

    // ...existing code...
    private boolean isValidHost(String host) {
        if (host == null) return false;
        host = host.trim();
        if (host.isEmpty() || host.length() > 253) return false;

        // simple IPv4 regex
        String ipv4 = "^(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)){3}$";
        // simple hostname regex (labels separated by dots, labels 1-63 chars, allowed characters a-zA-Z0-9-)
        String hostname = "^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";

        if (host.matches(ipv4) || host.matches(hostname)) {
            return true;
        }

        // as a fallback allow valid InetAddress parsing (covers IPv6)
        try {
            InetAddress.getByName(host);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String pingHost(String host) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: ping -n 1 <host>
            cmd.add("ping");
            cmd.add("-n");
            cmd.add("1");
            cmd.add(host);
        } else {
            // Unix-like: ping -c 1 <host>
            cmd.add("ping");
            cmd.add("-c");
            cmd.add("1");
            cmd.add(host);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = p.waitFor();
        output.append("Exit code: ").append(exitCode).append("\n");
        return output.toString();
    }
    // ...existing code...
}
