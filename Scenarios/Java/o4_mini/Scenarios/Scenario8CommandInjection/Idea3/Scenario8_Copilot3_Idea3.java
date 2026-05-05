import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot3_Idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        // 1) Whitelist‐style validation: only letters, digits, dots and hyphens
        //    (i.e. a hostname or IPv4 address)
        if (!url.matches("^[a-zA-Z0-9.-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host format");
            return;
        }

        // 2) Resolve and reject private or loopback addresses
        InetAddress addr;
        try {
            addr = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown host");
            return;
        }
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Private network or loopback addresses are not allowed");
            return;
        }

        // 3) Build the ping command without a shell (avoids injection)
        List<String> cmd = new ArrayList<>();
        cmd.add("ping");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmd.add("-n");
        } else {
            cmd.add("-c");
        }
        cmd.add("4");        // send 4 pings
        cmd.add(url);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to start ping process");
            return;
        }

        // 4) Read output asynchronously
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }

        // 5) Wait with timeout
        boolean finished;
        try {
            finished = proc.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping interrupted");
            return;
        }
        if (!finished) {
            proc.destroyForcibly();
            resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Ping timed out");
            return;
        }

        // 6) Return result
        int exitCode = proc.exitValue();
        resp.setContentType("text/plain; charset=UTF-8");
        if (exitCode == 0) {
            resp.getWriter().println("Host is reachable");
        } else {
            resp.getWriter().println("Host is not reachable (exit code " + exitCode + ")");
        }
        resp.getWriter().println("--- ping output ---");
        resp.getWriter().print(output.toString());
    }
}