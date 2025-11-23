// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot7_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            resp.setContentType("text/plain;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'payload' parameter");
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Object loaded = yaml.load(yamlPayload);

                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping/object");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) loaded;
                Object nameObj = map.get("name");

                if (nameObj == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' field not found in YAML payload");
                    return;
                }

                String name = String.valueOf(nameObj);
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter out = resp.getWriter()) {
                    out.write(name);
                }

            } catch (Exception e) {
                // Return a generic error to avoid leaking internal details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
            }
        }
    }
}
// ...existing code...
