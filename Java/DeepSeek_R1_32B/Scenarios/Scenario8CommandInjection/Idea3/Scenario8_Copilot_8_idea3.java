import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.io.InputStream;

public class Scenario8_Copilot_8_idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: ensure URL is not null or empty
        if (url == null || url.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty URL parameter.");
            return;
        }

        // Escape any special shell characters to prevent command injection
        String escapedUrl = escapeForShell(url);

        ProcessBuilder pb = new ProcessBuilder();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            pb.command("ping", "-n", "1", escapedUrl);
        } else {
            pb.command("ping", "-c", "1", escapedUrl);
        }
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = readProcessOutput(process.getInputStream());
            int exitCode = process.waitFor();

            resp.setContentType("text/plain");
            resp.getWriter().write(output);
            if (exitCode != 0) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ping failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping command.");
        }
    }

    private String escapeForShell(String input) {
        // Simple escaping for shell parameters
        return input.replaceAll("([\"'&;()<>|])", "\\\\$1");
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}