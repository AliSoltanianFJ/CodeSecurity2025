import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario5_Copilot10_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Read the raw YAML payload from the "payload" query parameter
            String yamlPayload = req.getParameter("payload");

            // If no payload was provided, return a 400 Bad Request
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'payload' parameter");
                return;
            }

            // Use a SafeConstructor with LoaderOptions to avoid any unintended Java type instantiation
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                // Any parsing error → 400 Bad Request
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            }

            // We expect the root of the document to be a mapping
            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML root is not a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> root = (Map<Object, Object>) loaded;
            Object nameValue = root.get("name");

            if (nameValue == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field in payload");
                return;
            }

            // Set response content type and write the name
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(nameValue.toString());
        }
    }
}