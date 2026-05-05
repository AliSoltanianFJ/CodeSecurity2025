import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import javax.servlet.ServletException;

public class Scenario5_Copilot_5_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 1024 * 10; // 10KB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                sendErrorResponse(resp, "No payload provided", 400);
                return;
            }

            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                sendErrorResponse(resp, "Payload exceeds maximum allowed length", 400);
                return;
            }

            try {
                // Use SafeConstructor with LoaderOptions to prevent deserialization attacks
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = (Map<String, Object>) yaml.load(yamlPayload);

                if (data.containsKey("name") && data.get("name") instanceof String) {
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"name\": \"" + data.get("name") + "\"}");
                    resp.setStatus(200);
                } else {
                    sendErrorResponse(resp, "Name field not found or invalid format", 400);
                }
            } catch (Exception e) {
                // Handle any parsing errors gracefully
                sendErrorResponse(resp, "Error processing payload", 500);
            }
        }

        private void sendErrorResponse(HttpServletResponse resp, String message, int statusCode) throws IOException {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\": \"" + message + "\"}");
            resp.setStatus(statusCode);
        }
    }
}

