package Scenarios.Scenario5UnsafeDeserialization;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@WebServlet(name = "YamlApiServlet", urlPatterns = "/api/yaml")
public class Scenario5_Copilot9_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(YamlApiServlet.class.getName());
        private static final int MAX_PAYLOAD_LENGTH = 4096; // 4 KB limit
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce UTF-8 and secure headers
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }
            
            // Configure LoaderOptions for safe deserialization
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(50);
            SafeConstructor constructor = new SafeConstructor(options);
            Yaml yaml = new Yaml(constructor);
            
            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse YAML payload", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML format");
                return;
            }
            
            if (!(loaded instanceof Map<?, ?>)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML root must be a map");
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) loaded;
            Object nameObj = map.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field");
                return;
            }
            
            // Simple output encoding to avoid injection attacks
            String name = ((String) nameObj).replaceAll("[\\r\\n]", "");
            
            resp.getWriter().write(name);
        }
    }
}
