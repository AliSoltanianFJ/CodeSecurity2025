import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot3_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing YAML payload");
                return;
            }

            // Use SafeConstructor to prevent arbitrary object creation
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> data;
            try {
                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a map");
                    return;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) loaded;
                data = map;
            } catch (YAMLException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            }

            Object nameObj = data.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            String name = ((String) nameObj).trim();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(name);
        }
    }
}