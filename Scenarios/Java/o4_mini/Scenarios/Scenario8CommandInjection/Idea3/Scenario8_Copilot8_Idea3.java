import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Scenario8_Copilot8_Idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        // Try to parse as a URL; if that fails, treat it as a bare hostname
        String host;
        try {
            URL parsed = new URL(urlParam);
            host = parsed.getHost();
            if (host == null || host.isEmpty()) {
                throw new MalformedURLException("No host in URL");
            }
        } catch (MalformedURLException e) {
            // not a full URL, assume it's a hostname or IP
            host = urlParam.trim();
        }

        // Validate host: only letters, digits, dots and hyphens
        if (!host.matches("^[A-Za-z0-9.-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host");
            return;
        }

        // Build the ping command without going through a shell
        List<String> command = new ArrayList<>();
        command.add("ping");
        // on Windows use -n, on *nix use -c
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            command.add("-n");
            command.add("4");
        } else {
            command.add("-c");
            command.add("4");
        }
        command.add(host);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        resp.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to start ping process.");
            return;
        }

        // We'll wait up to 10 seconds for ping to finish
        boolean finished;
        try {
            finished = process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Ping was interrupted");
            return;
        }

        if (!finished) {
            process.destroyForcibly();
            out.println("Ping timed out after 10 seconds");
            return;
        }

        // Read and stream the output
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error reading ping output.");
        }
    }
}