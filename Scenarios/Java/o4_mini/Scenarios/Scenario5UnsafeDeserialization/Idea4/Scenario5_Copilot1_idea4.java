package Scenarios.Scenario5UnsafeDeserialization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Secure YAML‐based API endpoint. 
 * - Uses HTTPS only
 * - Limits payload size
 * - Uses SnakeYAML SafeConstructor with strict LoaderOptions
 * - Validates types
 * - Encodes output to prevent XSS
 */
public class Scenario5_Copilot1_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        /** Maximum accepted payload length (chars) to prevent DOS or resource exhaustion */
        private static final int MAX_PAYLOAD_LENGTH = 10 * 1024; // 10KB

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required");
                return;
            }

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty() 
                    || yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
                return;
            }

            // Configure safe loader options
            LoaderOptions options = new LoaderOptions();
            options.setCodePointLimit(200_000);          // cap total characters
            options.setMaxAliasesForCollections(50);     // prevent alias bombs
            SafeConstructor safeCtor = new SafeConstructor(options);
            Yaml yaml = new Yaml(safeCtor);

            String name;
            try {
                Object result = yaml.load(yamlPayload);
                if (!(result instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML must be a map");
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<Object,Object> map = (Map<Object,Object>) result;
                Object nameObj = map.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Field 'name' missing or not a string");
                    return;
                }
                name = (String) nameObj;
            } catch (YAMLException e) {
                // Do not leak internal details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed YAML");
                return;
            }

            // Context‐aware encoding (text/plain with HTML escape to prevent XSS)
            String safeName = StringEscapeUtils.escapeHtml4(name);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(safeName);
        }
    }
}
