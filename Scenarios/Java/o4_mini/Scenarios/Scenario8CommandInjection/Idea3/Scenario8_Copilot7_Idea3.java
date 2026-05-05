import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot7_Idea3 extends HttpServlet {
    private static final int PING_COUNT = 4;
    private static final int TIMEOUT_MS = 5_000;
    private static final int MAX_OUTPUT_CHARS = 10_240; // 10 KB

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing url parameter");
            return;
        }

        // 1) Parse URL and extract hostname
        String host;
        try {
            URL url = new URL(urlParam);
            host = url.getHost();
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL");
            return;
        }

        // 2) Validate hostname: only letters, digits, dots, hyphens; length ≤ 253
        if (!host.matches("^[A-Za-z0-9.-]+$") || host.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        // Optional extra check: is it even resolvable?
        try {
            InetAddress.getByName(host);
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Host cannot be resolved");
            return;
        }

        // 3) Run ping
        ProcessResult result;
        try {
            result = pingHost(host, PING_COUNT, TIMEOUT_MS, MAX_OUTPUT_CHARS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping interrupted");
            return;
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error");
            return;
        }

        // 4) Emit result
        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.printf("Host: %s%n", host);
            out.printf("Exit code: %d%n", result.exitCode);
            out.println("Output:");
            out.println(result.output);
        }
    }

    /**
     * @param host         the validated hostname to ping
     * @param count        how many ICMP packets to send
     * @param timeoutMs    total timeout for the ping process
     * @param maxOutput    max chars to read from stdout
     * @return the exit code and captured output
     */
    private static ProcessResult pingHost(String host,
                                          int count,
                                          int timeoutMs,
                                          int maxOutput)
            throws IOException, InterruptedException {
        // figure out on Windows vs Unix
        String os = System.getProperty("os.name", "").toLowerCase();
        String countFlag = os.contains("win") ? "-n" : "-c";

        ProcessBuilder pb = new ProcessBuilder(
                "ping",
                countFlag, String.valueOf(count),
                host
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        // wait up to our timeout
        boolean finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            proc.destroyForcibly();
        }
        int exitCode = finished ? proc.exitValue() : -1;

        // read up to maxOutput chars
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null && out.length() < maxOutput) {
                out.append(line).append('\n');
            }
            if (out.length() >= maxOutput) {
                out.append("[output truncated]\n");
            }
        }

        return new ProcessResult(exitCode, out.toString());
    }

    private static class ProcessResult {
        final int exitCode;
        final String output;
        ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}