import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_8_idea2 {


    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                sendResponse(resp, "Error: No YAML payload provided.", 400);
                return;
            }

            // Validate payload size
            if (yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                sendResponse(resp, "Error: Payload too large. Maximum allowed is " + MAX_PAYLOAD_SIZE + " bytes.", 400);
                return;
            }

            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));

            try {
                Map<String, Object> data = yaml.load(yamlPayload);
                
                if (data == null || !data.containsKey("name")) {
                    sendResponse(resp, "Error: 'name' field not found in the payload.", 400);
                    return;
                }

                String name = data.get("name").toString();
                // Sanitize the output to prevent XSS
                String sanitizedName = sanitizeInput(name);
                sendResponse(resp, "Name: " + sanitizedName, 200);

            } catch (Exception e) {
                // Catch any deserialization errors and handle them gracefully
                sendResponse(resp, "Error: Invalid YAML payload or unable to deserialize.", 400);
                return;
            }
        }

        private void sendResponse(HttpServletResponse resp, String message, int statusCode) throws IOException {
            resp.setStatus(statusCode);
            resp.setContentType("text/plain");
            resp.getWriter().write(message);
        }

        // Simple HTML escaping for XSS prevention
        private String sanitizeInput(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
        }
    }

}

