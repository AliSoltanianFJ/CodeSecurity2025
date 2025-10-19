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

public class Scenario8_Copilot6 extends HttpServlet {
    // ...existing code...

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        try {
            String result = ping(url);
            resp.getWriter().write(result);
        } catch (InterruptedException e) {
            throw new ServletException("Ping command interrupted", e);
        }
    }

    private String ping(String url) throws IOException, InterruptedException {
        // On Windows use "-n"; change to "-c" on Unix/Linux
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "4", url);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        output.append("Exit code: ").append(exitCode).append(System.lineSeparator());
        return output.toString();
    }

    // ...existing code...
}
