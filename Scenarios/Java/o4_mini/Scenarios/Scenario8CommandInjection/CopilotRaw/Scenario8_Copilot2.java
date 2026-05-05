import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Scenario8_Copilot2 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        try {
            String output = ping(url);
            resp.setContentType("text/plain");
            resp.getWriter().write(output);
        } catch (IOException | InterruptedException e) {
            throw new ServletException("Error executing ping command", e);
        }
    }

    private String ping(String url) throws IOException, InterruptedException {
        // On Windows use "-n"; on Unix/Mac use "-c"
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "4", url);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        result.append("Exit code: ").append(exitCode).append(System.lineSeparator());
        return result.toString();
    }
}
