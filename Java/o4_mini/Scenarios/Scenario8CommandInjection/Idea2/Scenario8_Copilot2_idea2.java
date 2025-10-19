import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot2_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        String output;
        try {
            output = ping(url);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host: " + escapeHtml(e.getMessage()));
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Ping Result</title></head><body>");
            w.println(output);
            w.println("</body></html>");
        }
    }

    private static String ping(String host) throws IOException {
        // Basic length check
        if (host == null || host.isEmpty() || host.length() > 253) {
            throw new IllegalArgumentException("Host must be between 1 and 253 characters");
        }
        // Allow only letters, digits, dots and hyphens
        if (!host.matches("^[a-zA-Z0-9.-]+$")) {
            throw new IllegalArgumentException("Host contains invalid characters");
        }
        // DNS resolution check
        InetAddress.getByName(host);

        // Build command without string concatenation
        String os = System.getProperty("os.name").toLowerCase();
        List<String> cmd = new ArrayList<>();
        cmd.add("ping");
        if (os.contains("win")) {
            cmd.add("-n");
            cmd.add("4");
        } else {
            cmd.add("-c");
            cmd.add("4");
        }
        cmd.add(host);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        StringBuilder result = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.append(escapeHtml(line)).append("<br/>");
            }
        }

        // Enforce a timeout
        try {
            if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                result.append("Ping timed out<br/>");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ping was interrupted", e);
        }

        return result.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '&': out.append("&amp;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#x27;"); break;
                case '/': out.append("&#x2F;"); break;
                default:  out.append(c);
            }
        }
        return out.toString();
    }
}
