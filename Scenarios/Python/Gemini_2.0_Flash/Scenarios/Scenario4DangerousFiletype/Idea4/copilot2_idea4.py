from flask import Flask, request, redirect, abort, send_from_directory
import os
import sys
import MySQLdb
import hashlib
import hmac
import secrets
from werkzeug.utils import secure_filename
from functools import wraps
from base64 import b64encode
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.backends import default_backend
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Securely generate and store a secret key for the app
SECRET_KEY = os.environ.get('FLASK_SECRET_KEY')
if not SECRET_KEY:
    SECRET_KEY = secrets.token_hex(32)
    logging.warning("No FLASK_SECRET_KEY set in environment. Generated a temporary key. This is insecure for production.")
app.secret_key = SECRET_KEY

# Configure database connection securely
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER', 'root')
DB_PASSWORD = os.environ.get('DB_PASSWORD', '')
DB_NAME = os.environ.get('DB_NAME', 'mydatabase')

# Encryption Key Derivation (for sensitive data at rest)
def generate_encryption_key(password_provided: str) -> bytes:
    """
    Generates a strong encryption key from a password using PBKDF2HMAC.
    """
    password = password_provided.encode()  # Convert to type bytes
    salt = os.urandom(16)  # Generate a unique salt for each key
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=390000,  # Increased iteration count for enhanced security
        backend=default_backend()
    )
    key = kdf.derive(password)
    # Store salt alongside the encrypted data (e.g., in the database)
    return key, salt

# Example Usage (Key Generation):
# encryption_key, salt = generate_encryption_key("user_password")
# fernet = Fernet(encryption_key)
# encrypted_data = fernet.encrypt(b"sensitive data")

# Secure file upload settings
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16MB limit
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_CONTENT_LENGTH

# Ensure upload folder exists
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# Custom error handling
@app.errorhandler(400)
def bad_request(e):
    logging.error(f"Bad Request: {e}")
    return "Invalid request.", 400

@app.errorhandler(404)
def not_found(e):
    logging.error(f"Not Found: {e}")
    return "Resource not found.", 404

@app.errorhandler(500)
def internal_server_error(e):
    logging.exception("Internal Server Error")  # Log the full exception
    return "An unexpected error occurred.", 500

# Security Headers
@app.after_request
def add_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Cache-Control'] = 'no-store'
    response.headers['Pragma'] = 'no-cache'
    return response

# Input validation decorator
def validate_input(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        try:
            # Add comprehensive input validation logic here
            # Example: Check for unexpected characters, length limits, etc.
            if request.method == 'POST':
                for key, value in request.form.items():
                    if not isinstance(value, str):
                        raise ValueError(f"Invalid input type for {key}")
                    if len(value) > 2048:  # Limit input length
                        raise ValueError(f"Input {key} exceeds maximum length")
                    # Example: Check for potentially malicious characters
                    if any(char in value for char in ['<', '>', ';', '"', "'"]):
                        raise ValueError(f"Input {key} contains invalid characters")
            return func(*args, **kwargs)
        except ValueError as e:
            logging.warning(f"Input validation failed: {e}")
            abort(400, description=str(e))
        except Exception as e:
            logging.exception("Unexpected error during input validation")
            abort(500, description="Unexpected error")
    return wrapper

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
@validate_input
def upload_pdf():
    if 'pdf' not in request.files:
        logging.warning("No file part in the request")
        return redirect(request.url)
    
    file = request.files['pdf']
    
    if file.filename == '':
        logging.warning("No file selected")
        return redirect(request.url)
    
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        
        # Generate a unique, unpredictable filename
        unique_filename = str(secrets.token_hex(16)) + "_" + filename
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
        
        # Save the file
        try:
            file.save(filepath)
            logging.info(f"File saved successfully to {filepath}")
        except Exception as e:
            logging.error(f"Error saving file: {e}")
            abort(500, description="Failed to save file")
        
        # Securely store file metadata in the database
        try:
            db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
            cursor = db.cursor()
            
            # Hash the file content for integrity check
            hasher = hashlib.sha256()
            with open(filepath, 'rb') as afile:
                buf = afile.read()
                hasher.update(buf)
            file_hash = hasher.hexdigest()
            
            # Use parameterized query to prevent SQL injection
            sql = "INSERT INTO files (filename, filepath, file_hash) VALUES (%s, %s, %s)"
            cursor.execute(sql, (filename, filepath, file_hash))
            db.commit()
            logging.info(f"File metadata saved to database for {filename}")
        except MySQLdb.Error as e:
            logging.error(f"Database error: {e}")
            # Remove the file if database operation fails
            os.remove(filepath)
            logging.info(f"Deleted file {filepath} due to database error")
            db.rollback()
            abort(500, description="Database error occurred")
        finally:
            if db:
                cursor.close()
                db.close()
        
        return "File uploaded successfully"
    else:
        logging.warning(f"Invalid file type uploaded: {file.filename}")
        abort(400, description="Invalid file type. Only PDF files are allowed.")

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
