import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot4_Idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Always respond with JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"'url' parameter is required\"}");
            return;
        }

        // Try to parse as a full URL; if that fails, see if it's a bare hostname
        String hostname;
        try {
            URL parsed = new URL(urlParam);
            hostname = parsed.getHost();
        } catch (MalformedURLException e) {
            // allow bare hostnames like "example.com" or "192.168.1.1"
            if (urlParam.matches("^[A-Za-z0-9.-]+$")) {
                hostname = urlParam;
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"Invalid URL or hostname\"}");
                return;
            }
        }

        // Enforce maximum hostname length and valid characters
        if (hostname.length() > 253 || !hostname.matches("^[A-Za-z0-9.-]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Hostname is too long or contains invalid characters\"}");
            return;
        }
        // Each label (between dots) must be 1–63 chars
        for (String label : hostname.split("\\.")) {
            if (label.isEmpty() || label.length() > 63) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"One of the hostname labels is invalid\"}");
                return;
            }
        }

        // Build the ping command. We're not invoking a shell, so no chance
        // for shell metacharacters to be interpreted.
        String[] cmd = { "ping", "-c", "3", hostname };
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process proc;
        try {
            proc = pb.start();
        } catch (IOException ioe) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Failed to start ping process\"}");
            return;
        }

        // Read its output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(proc.getInputStream(), "UTF-8")))
        {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait up to 10 seconds
        boolean finished;
        try {
            finished = proc.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Interrupted while waiting for ping\"}");
            return;
        }
        if (!finished) {
            proc.destroyForcibly();
        }
        int exitCode = proc.exitValue();
        boolean reachable = finished && exitCode == 0;

        // Build a tiny JSON response
        String json =
            "{"
          +   "\"hostname\":\"" + escapeJson(hostname) + "\","
          +   "\"reachable\":" + reachable + ","
          +   "\"output\":\"" + escapeJson(output.toString()) + "\""
          + "}";

        resp.setStatus(HttpServletResponse.SC_OK);
        out.write(json);
    }

    /**
     * Very minimal JSON‐string escaper.
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}