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

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Set a secret key for the Flask app for CSRF protection
app.secret_key = secrets.token_hex(32)

# Define the upload folder and allowed extensions
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max upload size

# Create the uploads directory if it doesn't exist
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# Database configuration (move to environment variables in production)
DB_HOST = 'localhost'
DB_USER = 'your_db_user'
DB_PASSWORD = 'your_db_password'
DB_NAME = 'your_db_name'

# Hashing algorithm for file integrity
HASH_ALGORITHM = 'sha256'

# --- Security Helper Functions ---

def verify_hmac(filename, hmac_key, received_hmac):
    """Verify the HMAC of a file to ensure integrity."""
    calculated_hmac = calculate_hmac(filename, hmac_key)
    return hmac.compare_digest(calculated_hmac, received_hmac)

def calculate_hmac(filename, hmac_key):
    """Calculate the HMAC of a file."""
    hasher = hmac.new(hmac_key.encode('utf-8'), digestmod=hashlib.sha256)
    with open(filename, 'rb') as f:
        while True:
            chunk = f.read(4096)
            if not chunk:
                break
            hasher.update(chunk)
    return hasher.hexdigest()

def generate_hmac_key():
    """Generate a secure HMAC key."""
    return secrets.token_hex(32)

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def sanitize_filename(filename):
    """Sanitize the filename to prevent directory traversal."""
    filename = secure_filename(filename)
    if filename.startswith('..'):
        return None  # or raise an exception
    return filename

def log_activity(activity_type, details):
    """Log user activity for auditing purposes."""
    logging.info(f'{activity_type}: {details}')

def requires_auth(f):
    """Authentication decorator (example)."""
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not check_auth(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def check_auth(username, password):
    """Check if username/password combination is valid (example)."""
    # Replace with a secure authentication mechanism
    return username == 'admin' and password == 'secret'

def authenticate():
    """Sends a 401 response that enables basic auth."""
    return ('', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

def connect_db():
    """Establish a database connection."""
    try:
        db = MySQLdb.connect(host=DB_HOST, user=DB_USER, passwd=DB_PASSWORD, db=DB_NAME)
        return db
    except MySQLdb.Error as e:
        logging.error(f"Database connection error: {e}")
        abort(500)  # Internal Server Error

# --- End Security Helper Functions ---

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
@requires_auth  # Add authentication
def upload_pdf():
    """Upload a PDF file, perform security checks, and save it."""
    if 'pdf' not in request.files:
        log_activity('UPLOAD_FAILED', 'No file part')
        abort(400)  # Bad Request
    
    pdf = request.files['pdf']

    if pdf.filename == '':
        log_activity('UPLOAD_FAILED', 'No selected file')
        abort(400)  # Bad Request

    if pdf and allowed_file(pdf.filename):
        # Sanitize the filename
        filename = sanitize_filename(pdf.filename)
        if not filename:
            log_activity('UPLOAD_FAILED', f'Invalid filename: {pdf.filename}')
            abort(400)  # Bad Request

        # Generate a secure filename
        random_prefix = secrets.token_hex(16)
        filename = f"{random_prefix}_{filename}"
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

        # Save the file
        try:
            pdf.save(filepath)
            log_activity('UPLOAD_SUCCESS', f'File saved to {filepath}')
        except Exception as e:
            logging.error(f"File save error: {e}")
            log_activity('UPLOAD_FAILED', f'File save error: {e}')
            abort(500)  # Internal Server Error

        # Generate HMAC for integrity check
        hmac_key = generate_hmac_key()
        file_hmac = calculate_hmac(filepath, hmac_key)

        # Store file metadata in the database
        try:
            db = connect_db()
            cursor = db.cursor()
            
            # Use parameterized query to prevent SQL injection
            sql = "INSERT INTO files (filename, filepath, upload_date, file_hash, hmac_key) VALUES (%s, %s, NOW(), %s, %s)"
            cursor.execute(sql, (filename, filepath, file_hmac, hmac_key))
            db.commit()
            log_activity('DB_UPDATE', f'File metadata saved for {filename}')
        except MySQLdb.Error as e:
            logging.error(f"Database error: {e}")
            log_activity('DB_UPDATE_FAILED', f'Database error: {e}')
            db.rollback()
            abort(500)  # Internal Server Error
        finally:
            if db:
                cursor.close()
                db.close()

        return 'File uploaded successfully.'
    else:
        log_activity('UPLOAD_FAILED', f'Invalid file type: {pdf.filename}')
        abort(400)  # Bad Request

@app.route('/pdf/download/<filename>')
@requires_auth
def download_pdf(filename):
    """Download a PDF file, verifying its integrity."""
    try:
        db = connect_db()
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        sql = "SELECT filepath, file_hash, hmac_key FROM files WHERE filename = %s"
        cursor.execute(sql, (filename,))
        result = cursor.fetchone()

        if not result:
            log_activity('DOWNLOAD_FAILED', f'File not found: {filename}')
            abort(404)  # Not Found

        filepath, file_hash, hmac_key = result
        log_activity('DB_READ', f'File metadata retrieved for {filename}')
    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        log_activity('DB_READ_FAILED', f'Database error: {e}')
        abort(500)  # Internal Server Error
    finally:
        if db:
            cursor.close()
            db.close()

    # Verify HMAC before serving the file
    if not verify_hmac(filepath, hmac_key, file_hash):
        log_activity('DOWNLOAD_FAILED', f'HMAC verification failed for {filename}')
        abort(400)  # Bad Request
    
    log_activity('DOWNLOAD_SUCCESS', f'File downloaded: {filename}')
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename, as_attachment=True)

@app.errorhandler(400)
def bad_request(e):
    """Custom error handler for 400 errors."""
    return "Bad Request", 400

@app.errorhandler(401)
def unauthorized(e):
    """Custom error handler for 401 errors."""
    return "Unauthorized", 401

@app.errorhandler(404)
def not_found(e):
    """Custom error handler for 404 errors."""
    return "Not Found", 404

@app.errorhandler(500)
def internal_server_error(e):
    """Custom error handler for 500 errors."""
    return "Internal Server Error", 500

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
