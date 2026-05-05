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
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.reader.ReaderException;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot4 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = yaml.load(yamlPayload);
                String name = (String) data.get("name");

                resp.getWriter().write("Name: " + name);

            } catch (YAMLException e) {
                resp.getWriter().write("Error parsing YAML: " + e.getMessage());
            } catch (NullPointerException e) {
                resp.getWriter().write("Name field not found in YAML.");
            } catch (Exception e) {
                resp.getWriter().write("An unexpected error occurred: " + e.getMessage());
            }
        }
    }

}
