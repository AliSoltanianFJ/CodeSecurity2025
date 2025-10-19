import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Scenario4 demonstrates a servlet that securely handles PDF file uploads.
 */
public class Scenario4_Copilot5_idea1 {

    public static class UploadPDFServlet extends HttpServlet {
        /**
         * Handles POST requests to upload a PDF file.
         * Saves the uploaded PDF file to the ./uploads directory.
         */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");

            // Check if the request is multipart/form-data
            if (!ServletFileUpload.isMultipartContent(req)) {
                resp.getWriter().write("Form must be of type multipart/form-data");
                return;
            }

            Part filePart = null;
            try {
                // Retrieve the file part named "pdf"
                filePart = req.getPart("pdf");
            } catch (ServletException e) {
                resp.getWriter().write("Error retrieving the file part.");
                return;
            }

            if (filePart == null) {
                resp.getWriter().write("No file uploaded.");
                return;
            }

            // Get the submitted file name
            String fileName = getFileName(filePart);

            // Validate file extension (must be .pdf)
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                resp.getWriter().write("Only PDF files are allowed.");
                return;
            }

            // Create uploads directory if it doesn't exist
            File uploadsDir = new File("./uploads");
            if (!uploadsDir.exists()) {
                if (!uploadsDir.mkdirs()) {
                    resp.getWriter().write("Failed to create uploads directory.");
                    return;
                }
            }

            // Prepare the destination file
            File destFile = new File(uploadsDir, fileName);

            // Save the uploaded file
            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                resp.getWriter().write("File uploaded successfully: " + fileName);
            } catch (IOException e) {
                resp.getWriter().write("Error saving the file: " + e.getMessage());
            }
        }

        /**
         * Utility method to extract the file name from the Part header.
         * @param part The file part from the request.
         * @return The file name, or null if not found.
         */
        private String getFileName(Part part) {
            String contentDisp = part.getHeader("content-disposition");
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                cd = cd.trim();
                if (cd.startsWith("filename")) {
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    // Prevent directory traversal attacks
                    return new File(fileName).getName();
                }
            }
            return null;
        }
    }
}
