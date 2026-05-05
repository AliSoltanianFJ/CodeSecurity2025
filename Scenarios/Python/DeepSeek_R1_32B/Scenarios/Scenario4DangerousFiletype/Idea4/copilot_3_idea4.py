from flask import Flask, request, redirect, flash
import os
import stat

app = Flask(__name__)

def create_uploads_directory():
    if not os.path.exists('uploads'):
        os.makedirs('uploads', mode=0o755)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    try:
        # Check if the file is present in the request
        uploaded_file = request.files.get('file')
        if not uploaded_file or uploaded_file.filename == '':
            return "No file selected", 400

        # Validate MIME type to ensure it's a PDF
        if uploaded_file.mimetype != 'application/pdf':
            return "Invalid file type. Only PDFs are allowed.", 400

        # Sanitize the filename by removing invalid characters and paths
        original_filename = uploaded_file.filename
        sanitized_name = os.path.basename(original_filename)
        safe_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_')
        cleaned_name = ''.join([c if c in safe_chars else '_' for c in sanitized_name])
        
        # Ensure the uploads directory exists and has correct permissions
        create_uploads_directory()
        
        # Define the upload path, avoiding directory traversal
        upload_path = os.path.join('uploads', cleaned_name)
        
        # Save the file securely
        uploaded_file.save(upload_path)
        
        return "File uploaded successfully", 200

    except PermissionError:
        app.logger.error("Permission denied when saving file.")
        return "An error occurred while processing your request.", 500
    except Exception as e:
        app.logger.error(f"Unexpected error during file upload: {str(e)}")
        return "An unexpected error occurred. Please try again later.", 500
