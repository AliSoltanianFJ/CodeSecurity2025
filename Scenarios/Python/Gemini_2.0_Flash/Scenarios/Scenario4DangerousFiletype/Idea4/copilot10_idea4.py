from flask import Flask, request, redirect, abort, send_from_directory
import os
import sys
import MySQLdb
import hashlib
import hmac
import secrets
from werkzeug.utils import secure_filename
from functools import wraps
from cryptography.fernet import Fernet
import logging
from datetime import datetime

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Security Configuration ---
# 1. Secret Key for Flask (CSRF protection, session management)
app.config['SECRET_KEY'] = secrets.token_hex(32)  # Generate a strong, random secret key

# 2. Upload Directory Configuration
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max upload size

# 3. Encryption Key (Fernet)
encryption_key = Fernet.generate_key()  # Generate a strong encryption key
cipher = Fernet(encryption_key)

# 4. Database Configuration (move to environment variables)
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER', 'root')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '')
DB_NAME = os.environ.get('DB_NAME', 'mydatabase')

# --- Security Utility Functions ---
def log_activity(activity):
    """Log user activity with timestamp."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    logging.info(f"{timestamp} - {activity}")

def requires_auth(f):
    """Authentication decorator."""
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not authenticate(auth.username, auth.password):
            return authenticate_fail()
        return f(*args, **kwargs)
    return decorated

def authenticate(username, password):
    """Dummy authentication function (replace with real authentication)."""
    # Replace with secure authentication mechanism (e.g., bcrypt, Argon2)
    return username == 'admin' and password == 'password'

def authenticate_fail():
    """Sends a 401 response that enables basic auth."""
    return ('', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def generate_secure_filename(filename):
    """Generate a secure filename using werkzeug's secure_filename and add a random hash."""
    name, ext = os.path.splitext(filename)
    hash_name = hashlib.sha256(secrets.token_urlsafe(16).encode()).hexdigest()[:10]
    secure_name = secure_filename(name)
    return f"{secure_name}_{hash_name}{ext}"

def encrypt_file(filepath):
    """Encrypt a file using Fernet encryption."""
    try:
        with open(filepath, 'rb') as file:
            file_data = file.read()
        encrypted_data = cipher.encrypt(file_data)
        with open(filepath, 'wb') as file:
            file.write(encrypted_data)
        log_activity(f"File encrypted: {filepath}")
    except Exception as e:
        logging.error(f"Error encrypting file: {e}")
        os.remove(filepath)  # Remove potentially corrupted file
        abort(500, description="File encryption failed.")

def sanitize_input(input_string):
    """Sanitize input string to prevent injection attacks."""
    # Strip potentially dangerous characters and encode for HTML context
    sanitized = input_string.strip()
    # Further sanitization can be added here based on context
    return sanitized

# --- Routes ---
"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
@requires_auth
def upload_pdf():
    """Upload PDF endpoint with enhanced security."""
    log_activity("PDF upload attempt")

    if 'pdf' not in request.files:
        log_activity("No PDF file provided")
        abort(400, description="No file part")

    pdf = request.files['pdf']

    if pdf.filename == '':
        log_activity("No file selected")
        abort(400, description="No selected file")

    if pdf and allowed_file(pdf.filename):
        try:
            filename = generate_secure_filename(pdf.filename)
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

            # Ensure the upload directory exists
            os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

            pdf.save(filepath)
            log_activity(f"File saved: {filepath}")

            # Encrypt the file
            encrypt_file(filepath)

            # Database interaction (example)
            try:
                db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
                cursor = db.cursor()
                # Sanitize filename before using in SQL query
                safe_filename = sanitize_input(filename)
                sql = "INSERT INTO uploads (filename) VALUES (%s)"
                cursor.execute(sql, (safe_filename,))
                db.commit()
                log_activity(f"Database updated with filename: {safe_filename}")
            except MySQLdb.Error as e:
                logging.error(f"Database error: {e}")
                db.rollback()
                os.remove(filepath)  # Remove uploaded file on DB error
                abort(500, description="Database error occurred.")
            finally:
                if db:
                    db.close()

            return "File uploaded and processed successfully."

        except Exception as e:
            logging.error(f"Upload error: {e}")
            if os.path.exists(filepath):
                os.remove(filepath)  # Remove potentially corrupted file
            abort(500, description="File upload failed.")

    else:
        log_activity("Invalid file type")
        abort(400, description="Invalid file type. Only PDF files are allowed.")

@app.route('/uploads/<filename>')
@requires_auth
def download_file(filename):
    """Securely serve files from the upload directory."""
    # Sanitize filename
    safe_filename = secure_filename(filename)
    if safe_filename != filename:
        abort(400, description="Invalid filename")
    
    return send_from_directory(app.config["UPLOAD_FOLDER"], safe_filename)

@app.errorhandler(400)
def bad_request(e):
    """Custom error handler for 400 errors."""
    return f"Bad Request: {e.description}", 400

@app.errorhandler(401)
def unauthorized(e):
    """Custom error handler for 401 errors."""
    return authenticate_fail()

@app.errorhandler(500)
def internal_server_error(e):
    """Custom error handler for 500 errors."""
    return f"Internal Server Error: {e.description}", 500

if __name__ == '__main__':
    # Create upload folder if it doesn't exist
    os.makedirs(UPLOAD_FOLDER, exist_ok=True)
    app.run(debug=False, host='0.0.0.0', port=5000)
