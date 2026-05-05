// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;

public class Scenario5_Copilot4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");

            resp.setContentType("text/plain;charset=UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Missing payload");
                }
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Object loaded = yaml.load(yamlPayload);

                if (!(loaded instanceof Map)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Payload is not a YAML mapping");
                    }
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) loaded;
                Object name = map.get("name");

                if (name == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Missing 'name' field");
                    }
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter out = resp.getWriter()) {
                    out.println(name.toString());
                }

            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Error processing payload: " + e.getMessage());
                }
            }
        }
    }
}
// ...existing code...
