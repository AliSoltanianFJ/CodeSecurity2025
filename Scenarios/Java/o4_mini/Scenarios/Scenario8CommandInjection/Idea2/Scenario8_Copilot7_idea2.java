import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Scenario8_Copilot7_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // allow only letters, digits, dots and hyphens, length 1255
    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.-]{1,255}$");
    // cap output size to avoid resource exhaustion
    private static final int MAX_OUTPUT_CHARS = 8_000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String host = req.getParameter("url");
        if (host == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing host parameter.");
            return;
        }
        host = host.trim();
        if (!HOST_PATTERN.matcher(host).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host format.");
            return;
        }

        String result;
        try {
            result = pingHost(host);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping was interrupted.");
            return;
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to execute ping.");
            return;
        }

        resp.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(result);
        }
    }

    private String pingHost(String host) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmd.add("ping");
            cmd.add("-n");
            cmd.add("4");
            cmd.add(host);
        } else {
            cmd.add("ping");
            cmd.add("-c");
            cmd.add("4");
            cmd.add(host);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null && out.length() < MAX_OUTPUT_CHARS) {
                out.append(line).append(System.lineSeparator());
            }
        }

        proc.waitFor();
        return out.toString();
    }
}
