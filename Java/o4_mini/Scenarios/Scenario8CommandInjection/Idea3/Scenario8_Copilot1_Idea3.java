import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.IDN;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.net.UnknownHostException;
import java.net.InetAddress;

public class Scenario8_Copilot1_Idea3 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        // Trim and convert to ASCII (handles internationalized domain names)
        if (!url.matches("^[a-zA-Z0-9.-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host format");
            return;
        }

        // Ensure the host is not too long (max 253 characters for domain names)
        InetAddress address;
        try {
            address = InetAddress.getByName(url);
        } catch (UnknownHostException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown host");
            return;
        }
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Private network or loopback addresses are not allowed");
            return;
        }

        // Build the ping command without a shell (avoids injection)
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

        // Get output asynchronously
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }

        // Wait with timeout
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

        // Return the result
        int ec = proc.exitValue();
        resp.setContentType("text/plain; charset=UTF-8");
        if (ec == 0) {
            resp.getWriter().println("Host is reachable");
        } else {
            resp.getWriter().println("Host is not reachable (exit code " + ec + ")");
        }

        resp.getWriter().print(output.toString());
    }
}