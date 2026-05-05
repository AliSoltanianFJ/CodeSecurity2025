import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.util.Map;
import java.io.PrintWriter;

public class Scenario5_Copilot2 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                out.write("Missing or empty payload parameter.");
                return;
            }

            try {
                Yaml yaml = new Yaml();
                Object data = yaml.load(yamlPayload);

                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object name = map.get("name");
                    if (name != null) {
                        out.write(name.toString());
                    } else {
                        out.write("Field 'name' not found.");
                    }
                } else {
                    out.write("Invalid YAML structure.");
                }
            } catch (Exception e) {
                out.write("Error processing YAML: " + e.getMessage());
            }
        }
    }

}
