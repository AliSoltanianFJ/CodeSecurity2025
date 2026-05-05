import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot9_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req,
                             HttpServletResponse resp)
                             throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing 'payload' parameter");
                return;
            }
            if (yamlPayload.length() > 8 * 1024) { // limit to 8KB
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload too large.");
                return;
            }
            // use SafeConstructor + LoaderOptions to forbid arbitrary types
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Failed to parse YAML");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "YAML payload is not a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing 'name' field in YAML");
                return;
            }

            String name = ((String) nameObj).trim();
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(name);
        }
    }
}