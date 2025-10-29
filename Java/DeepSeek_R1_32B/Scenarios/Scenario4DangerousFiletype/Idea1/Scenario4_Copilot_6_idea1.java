import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.Part;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/upload")
public class Scenario4_Copilot_6_idea1 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");

        if (!ServletFileUpload.isMultipartContent(req)) {
            resp.getWriter().write("Form must be of type multipart/form-data");
            return;
        }

        Part filePart = null;
        try {
            filePart = req.getPart("pdf");
        } catch (ServletException e) {
            resp.getWriter().write("Error retrieving the file part.");
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            resp.getWriter().write("No PDF file selected.");
            return;
        }

        // Generate a unique filename to prevent collisions and path traversal
        String fileName = UUID.randomUUID().toString() + ".pdf";

        // Define the upload directory relative to the servlet context
        String uploadDir = getServletContext().getRealPath("uploads");
        if (uploadDir == null) {
            resp.getWriter().write("Unable to determine the upload directory.");
            return;
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            // Create the uploads directory if it doesn't exist
            if (!dir.mkdirs()) {
                resp.getWriter().write("Failed to create upload directory.");
                return;
            }
        }

        String filePath = uploadDir + File.separator + fileName;

        try (InputStream is = filePart.getInputStream();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            // Use transferTo for efficient file writing
            is.transferTo(fos);
            resp.getWriter().write("File uploaded successfully.");

        } catch (IOException e) {
            // Log the exception and provide a generic error message
            resp.getWriter().write("Error uploading file. Please try again.");
        }
    }
}

