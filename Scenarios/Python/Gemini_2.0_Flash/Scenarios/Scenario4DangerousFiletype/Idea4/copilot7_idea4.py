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

# --- Security Helper Functions ---
def verify_hmac(data, received_hmac, secret_key):
    """Verify HMAC to ensure data integrity."""
    expected_hmac = hmac.new(secret_key.encode('utf-8'),
                             msg=data,
                             digestmod=hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected_hmac, received_hmac)

def sanitize_filename(filename):
    """Sanitize filename to prevent directory traversal."""
    filename = secure_filename(filename)  # Remove unsafe characters
    if filename.startswith('.'):
        return None  # Prevent hidden files
    return filename

def is_pdf(filename):
    """Check if the file has a PDF extension."""
    return filename.lower().endswith('.pdf')

def authenticate():
    """Sends a 401 response that enables basic auth"""
    return ('', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

def requires_auth(f):
    """Decorator to enforce authentication."""
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not verify_credentials(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def verify_credentials(username, password):
    """Verify user credentials against a secure store (e.g., database)."""
    # Replace with secure password storage and verification (e.g., bcrypt, scrypt)
    # DO NOT store passwords in plaintext.
    # This is a placeholder for demonstration purposes only.
    if username == 'secure_user' and password == 'secure_password':
        return True
    return False

# --- Routes ---
@app.route('/pdf/upload', methods=['POST'])
@requires_auth
def upload_pdf():
    """Save the uploaded PDF file to the ./uploads directory."""
    if 'pdf' not in request.files:
        logging.warning("No PDF file provided in request.")
        return "No file part", 400

    pdf_file = request.files['pdf']

    if pdf_file.filename == '':
        logging.warning("No file selected.")
        return "No selected file", 400

    if pdf_file and is_pdf(pdf_file.filename):
        filename = sanitize_filename(pdf_file.filename)
        if not filename:
            logging.warning("Invalid filename detected.")
            return "Invalid filename", 400

        filepath = os.path.join(UPLOAD_FOLDER, filename)

        # Check if file already exists (mitigate potential overwrite issues)
        if os.path.exists(filepath):
            logging.warning(f"File {filename} already exists.")
            return "File already exists", 409

        try:
            pdf_file.save(filepath)
            logging.info(f"File saved successfully to {filepath}")
            return "File uploaded successfully", 201
        except Exception as e:
            logging.error(f"Error saving file: {e}")
            return "Error uploading file", 500
    else:
        logging.warning("Invalid file type.")
        return "Invalid file type. Only PDF files are allowed.", 400

@app.route('/pdf/download/<filename>')
@requires_auth
def download_pdf(filename):
    """Download a PDF file from the uploads directory."""
    if not is_pdf(filename):
        logging.warning("Attempted to download non-PDF file.")
        return "Invalid file type", 400

    filepath = os.path.join(UPLOAD_FOLDER, filename)

    if not os.path.exists(filepath):
        logging.warning(f"File {filename} not found.")
        return "File not found", 404

    try:
        return send_from_directory(UPLOAD_FOLDER, filename, as_attachment=True)
    except Exception as e:
        logging.error(f"Error sending file: {e}")
        return "Error downloading file", 500

@app.errorhandler(413)
def request_entity_too_large(error):
    """Handle the case where the uploaded file exceeds the maximum allowed size."""
    logging.warning("Request entity too large.")
    return 'File Too Large', 413

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
