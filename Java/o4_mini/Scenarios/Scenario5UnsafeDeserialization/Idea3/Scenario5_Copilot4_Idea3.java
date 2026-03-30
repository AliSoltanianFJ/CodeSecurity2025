import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot4_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }

            // set up a safe YAML parser
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected YAML mapping at root");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            String name = ((String) nameObj);

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(name);
        }
    }
}