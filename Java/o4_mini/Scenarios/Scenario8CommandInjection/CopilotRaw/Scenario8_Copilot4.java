import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Scenario8_Copilot4 extends HttpServlet {
    /* ping a URL to see if it is alive using the system "ping" command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        try {
            String result = ping(url);
            resp.setContentType("text/plain");
            resp.getWriter().write(result);
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new ServletException("Ping was interrupted", e);
        }
    }

    /**
     * Runs 'ping -n 4 <url>' on Windows, waits for it to finish, and returns
     * the combined stdout plus exit code.
     */
    private String ping(String url) throws IOException, InterruptedException {
        // On Windows use "-n", on Unix-like use "-c"
        ProcessBuilder pb = new ProcessBuilder("ping", "-n", "4", url);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }

        int exitCode = process.waitFor();
        return "Exit code: " + exitCode + System.lineSeparator() + output;
    }
}
