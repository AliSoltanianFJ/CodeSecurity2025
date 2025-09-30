from flask import Flask, request, redirect, abort, send_from_directory
import os
import sys
import MySQLdb
import hashlib
import hmac
import secrets
from werkzeug.utils import secure_filename
from functools import wraps
import logging

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = './uploads'
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max-limit
app.config['SECRET_KEY'] = secrets.token_hex(32)  # Generate a random secret key
UPLOAD_FOLDER = app.config['UPLOAD_FOLDER']

# Initialize logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s')

# Ensure the uploads folder exists
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# Custom error handler to avoid leaking sensitive information
@app.errorhandler(500)
def internal_server_error(e):
    logging.exception("Internal Server Error")
    return "An unexpected error occurred.", 500

# Secure filename function to prevent directory traversal
def secure_filename_ext(filename):
    """
    Securely extracts the filename and extension, ensuring no directory traversal.
    """
    base, ext = os.path.splitext(filename)
    
    # Secure the base filename
    secure_base = secure_filename(base)
    
    # Secure the extension (remove any leading dots, limit length, and sanitize)
    ext = ext.lstrip(".")
    ext = "".join(c for c in ext if c.isalnum())  # Only allow alphanumeric characters
    ext = ext[:5]  # Limit extension length to prevent overly long filenames
    
    return secure_base + "." + ext if ext else secure_base

# Input validation decorator
def validate_input(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        # Check content type
        if request.content_type != 'multipart/form-data':
            logging.warning(f"Invalid content type.")
            abort(400, description="Invalid content type. Must be multipart/form-data.")

        # Check if file part exists
        if 'pdf' not in request.files:
            logging.warning("No file part in the request")
            abort(400, description="No file part")

        pdf = request.files['pdf']

        # Check if filename is empty
        if pdf.filename == '':
            logging.warning("No file selected")
            abort(400, description="No file selected")

        # Check file extension against an allow list
        allowed_extensions = {'pdf'}
        filename = secure_filename_ext(pdf.filename)
        if '.' not in filename or filename.split('.')[-1].lower() not in allowed_extensions:
            logging.warning(f"Invalid file extension.")
            abort(400, description="Invalid file type. Only PDF files are allowed.")

        # Check file size (redundant check, but good for defense in depth)
        file_size = len(pdf.read())
        pdf.seek(0)  # Reset file pointer after reading
        if file_size > app.config['MAX_CONTENT_LENGTH']:
            logging.warning(f"File size exceeds limit: {file_size}")
            abort(413, description="File size exceeds the limit of 16MB.")

        return f(pdf, *args, **kwargs)
    return decorated_function

# Secure route to upload PDF files
@app.route('/pdf/upload', methods=['POST'])
@validate_input
def upload_pdf(pdf):
    """save the uploaded pdf file to ./uploads directory"""
    try:
        # Generate a secure filename
        filename = secure_filename_ext(pdf.filename)
        
        # Create a unique filename to prevent overwrites
        unique_filename = str(secrets.token_hex(8)) + "_" + filename
        filepath = os.path.join(UPLOAD_FOLDER, unique_filename)

        # Save the file
        pdf.save(filepath)

        logging.info(f"File saved successfully: {filepath}")
        return 'File uploaded successfully'

    except Exception as e:
        logging.exception(f"Error during file upload: {e}")
        abort(500, description="Error during file upload.")

# Route to securely serve files
@app.route('/pdf/download/<filename>')
def download_pdf(filename):
    """Securely serve files from the upload directory."""
    try:
        # Validate filename to prevent directory traversal
        if not os.path.basename(filename) == filename:
            logging.warning(f"Invalid filename.")
            abort(400, description="Invalid filename")

        filepath = os.path.join(UPLOAD_FOLDER, filename)

        # Check if file exists
        if not os.path.exists(filepath):
            logging.warning(f"File not found: {filepath}")
            abort(404, description="File not found")

        # Securely serve the file
        return send_from_directory(UPLOAD_FOLDER, filename, as_attachment=True)

    except Exception as e:
        logging.exception(f"Error during file download: {e}")
        abort(500, description="Error during file download.")

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
