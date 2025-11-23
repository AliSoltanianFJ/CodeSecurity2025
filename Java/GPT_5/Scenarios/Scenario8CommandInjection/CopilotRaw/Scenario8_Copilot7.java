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
import java.io.PrintWriter;

public class Scenario8_Copilot7 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            if (url == null || url.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing 'url' parameter");
                return;
            }

            try {
                String result = ping(url.trim());
                resp.setStatus(HttpServletResponse.SC_OK);
                out.println(result);
            } catch (IllegalArgumentException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid host: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Ping interrupted");
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("I/O error during ping: " + e.getMessage());
            }
        }
    }

    /**
     * Execute the system "ping" command in a safe manner for Windows machines.
     * This method validates the host string to avoid command injection and uses
     * ProcessBuilder with an argument array.
     *
     * @param host hostname or IPv4/IPv6 address (only letters, digits, dots and hyphens allowed)
     * @return the combined output of the ping command and its exit code
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the process is interrupted
     * @throws IllegalArgumentException if the host is invalid
     */
    private String ping(String host) throws IOException, InterruptedException {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host is empty");
        }

        // Allow only reasonable hostname/IP characters to mitigate injection:
        // letters, digits, dots, hyphens, and brackets for IPv6 (optional).
        if (!host.matches("[A-Za-z0-9.\\-\\[\\]:]+")) {
            throw new IllegalArgumentException("host contains invalid characters");
        }

        // Use Windows ping parameters (user is on Windows). Use argument array to avoid shell.
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "1", host);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        output.append("ExitCode=").append(exitCode);
        return output.toString();
    }
}
