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

# 3. Encryption Key (for sensitive data at rest)
encryption_key = Fernet.generate_key()
cipher = Fernet(encryption_key)

# 4. Database Configuration (replace with your actual credentials)
db_config = {
    'host': 'localhost',
    'user': 'your_user',
    'password': 'your_password',
    'database': 'your_database'
}

# --- Security Utility Functions ---

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
    # TODO: Replace with secure authentication mechanism (e.g., bcrypt, Argon2)
    return username == 'admin' and password == 'password'

def authenticate_fail():
    """Sends a 401 response that enables basic auth."""
    return ('', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def generate_secure_filename(filename):
    """Generate a secure filename using Werkzeug's secure_filename and add a UUID."""
    name, ext = os.path.splitext(filename)
    secure_name = secure_filename(name)
    return f"{secure_name}_{secrets.uuid4().hex}{ext}"

def hash_file(filepath):
    """Generate a SHA256 hash of the file."""
    hasher = hashlib.sha256()
    with open(filepath, 'rb') as file:
        while True:
            chunk = file.read(4096)
            if not chunk:
                break
            hasher.update(chunk)
    return hasher.hexdigest()

def encrypt_data(data: bytes) -> bytes:
    """Encrypt data using Fernet."""
    return cipher.encrypt(data)

def decrypt_data(encrypted_data: bytes) -> bytes:
    """Decrypt data using Fernet."""
    return cipher.decrypt(encrypted_data)

def store_file_metadata(filename, file_hash, upload_time):
    """Store file metadata in the database."""
    try:
        conn = MySQLdb.connect(**db_config)
        cursor = conn.cursor()
        sql = "INSERT INTO file_metadata (filename, file_hash, upload_time) VALUES (%s, %s, %s)"
        cursor.execute(sql, (filename, file_hash, upload_time))
        conn.commit()
    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        # It's generally bad practice to expose specific error details to the user.
        # Instead, provide a generic error message.
        abort(500, description="Failed to store file metadata.")
    finally:
        if conn:
            cursor.close()
            conn.close()

# --- Routes ---
@app.route('/pdf/upload', methods=['POST'])
@requires_auth
def upload_pdf():
    """Save the uploaded PDF file to the ./uploads directory."""

    # 1. Check if the file is present in the request
    if 'pdf' not in request.files:
        abort(400, description="No file part")

    p = request.files['pdf']

    # 2. Check if the file is empty
    if p.filename == '':
        abort(400, description="No file selected")

    # 3. Process the file if it's present and allowed
    if p and allowed_file(p.filename):
        # 4. Generate a secure filename
        filename = generate_secure_filename(p.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

        # 5. Read the file content
        file_content = p.read()

        # 6. Hash the file content for integrity check
        file_hash = hash_file(filepath)

        # 7. Encrypt the file content
        encrypted_content = encrypt_data(file_content)

        # 8. Save the encrypted file
        try:
            with open(filepath, 'wb') as f:
                f.write(encrypted_content)
        except IOError as e:
            logging.error(f"IOError: {e}")
            abort(500, description="Failed to save the file.")

        # 9. Store file metadata in the database
        upload_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        store_file_metadata(filename, file_hash, upload_time)

        # 10. Return a success message
        return "File uploaded successfully."

    else:
        abort(400, description="Invalid file type. Only PDF files are allowed.")

@app.route('/pdf/download/<filename>')
@requires_auth
def download_pdf(filename):
    """Download a PDF file."""
    filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

    # 1. Check if the file exists
    if not os.path.exists(filepath):
        abort(404, description="File not found")

    try:
        # 2. Read the encrypted content
        with open(filepath, 'rb') as f:
            encrypted_content = f.read()

        # 3. Decrypt the content
        decrypted_content = decrypt_data(encrypted_content)

        # 4. Send the decrypted file
        return send_from_directory(app.config['UPLOAD_FOLDER'], filename, as_attachment=True)

    except IOError as e:
        logging.error(f"IOError: {e}")
        abort(500, description="Failed to read the file.")
    except Exception as e:
        logging.error(f"Decryption error: {e}")
        abort(500, description="Failed to decrypt the file.")

@app.errorhandler(400)
def bad_request(e):
    """Custom error handler for 400 errors."""
    return f"Bad Request: {e.description}", 400

@app.errorhandler(404)
def not_found(e):
    """Custom error handler for 404 errors."""
    return f"Not Found: {e.description}", 404

@app.errorhandler(500)
def internal_server_error(e):
    """Custom error handler for 500 errors."""
    return f"Internal Server Error: {e.description}", 500

if __name__ == '__main__':
    # Create the uploads directory if it doesn't exist
    if not os.path.exists(UPLOAD_FOLDER):
        os.makedirs(UPLOAD_FOLDER)

    # Start the Flask app in debug mode (for development only!)
    app.run(debug=False)
