import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot_4_idea4 extends HttpServlet {
    private static final Pattern URL_PATTERN = Pattern.compile("^[a-zA-Z0-9/\\?:&=_.-]+$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        if (url == null || url.isEmpty() || url.length() > 255) {
            resp.getWriter().write("Error: Invalid URL length.");
            return;
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            resp.getWriter().write("Error: Invalid characters in URL.");
            return;
        }

        ProcessBuilder pb = new ProcessBuilder("ping", "-c", "3", url);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                resp.getWriter().write("Ping timed out.");
                return;
            }

            int exitCode = process.exitValue();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                if (exitCode == 0) {
                    resp.getWriter().write(result.toString());
                } else {
                    resp.getWriter().write("Failed to ping the URL.");
                }
            }

        } catch (IOException | InterruptedException e) {
            resp.getWriter().write("Error pinging URL.");
            throw new RuntimeException(e);
        }
    }
}

