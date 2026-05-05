from flask import Flask, request, redirect, abort, send_from_directory
import os
import sys
import MySQLdb
import hashlib
import hmac
import secrets
from werkzeug.utils import secure_filename
from functools import wraps

app = Flask(__name__)

# Configure upload directory and allowed extensions
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max-limit
app.config['SECRET_KEY'] = secrets.token_hex(32)  # Generate a strong secret key

# Initialize database connection parameters (DO NOT HARDCODE in real application)
DB_HOST = 'localhost'
DB_USER = 'your_db_user'
DB_PASSWORD = 'your_db_password'
DB_NAME = 'your_db_name'

# Create upload directory if it doesn't exist
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# --- Security Helper Functions ---
def verify_hmac(data, received_hmac, secret_key):
    """Verify HMAC to ensure data integrity."""
    expected_hmac = hmac.new(secret_key.encode('utf-8'), data.encode('utf-8'), hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected_hmac, received_hmac)

def requires_auth(f):
    """Authentication decorator."""
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not verify_credentials(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def authenticate():
    """Sends a 401 response that enables basic auth."""
    return ('', 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})

def verify_credentials(username, password):
    """Verify user credentials against a secure store (e.g., database)."""
    # Replace with secure password hashing and database lookup
    # NEVER store passwords in plaintext
    # Example using hashlib.sha256 (use bcrypt or scrypt in production)
    expected_password_hash = "your_hashed_password"  # Replace with actual hashed password from DB
    hashed_input_password = hashlib.sha256(password.encode('utf-8')).hexdigest()
    return username == "admin" and hashed_input_password == expected_password_hash

def is_safe_filename(filename):
    """Check if the filename is safe."""
    basename = os.path.basename(filename)
    if not basename:
        return False
    
    normalized_filename = os.path.normpath(basename)
    if normalized_filename.startswith('.'):
        return False
    
    return True

def allowed_file(filename):
    """Check if the file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def log_error(message):
    """Securely log errors without including user-provided data."""
    # Log to a secure location with appropriate permissions
    with open('error.log', 'a') as logfile:
        logfile.write(message + '\n')

# --- Routes ---
@app.route('/pdf/upload', methods=['POST'])
@requires_auth
def upload_pdf():
    """Save the uploaded PDF file to the ./uploads directory."""
    if 'pdf' not in request.files:
        log_error("Request missing PDF file")
        abort(400, description="No PDF file provided")

    pdf = request.files['pdf']

    if pdf.filename == '':
        log_error("Request with empty filename")
        abort(400, description="No file selected")

    if not allowed_file(pdf.filename):
        log_error(f"Invalid file extension: {pdf.filename}")
        abort(400, description="Invalid file type. Only PDF files are allowed.")
    
    if not is_safe_filename(pdf.filename):
        log_error(f"Unsafe filename: {pdf.filename}")
        abort(400, description="Unsafe filename")

    try:
        filename = secure_filename(pdf.filename)
        if filename is None:
            log_error(f"Filename could not be secured: {pdf.filename}")
            abort(500, description="Could not secure filename")
        
        # Ensure filename uniqueness to prevent overwrites
        unique_filename = str(secrets.token_hex(8)) + "_" + filename
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)

        # Save the file
        pdf.save(filepath)

        # Database interaction (using parameterized query)
        try:
            db = MySQLdb.connect(host=DB_HOST, user=DB_USER, password=DB_PASSWORD, database=DB_NAME)
            cursor = db.cursor()
            
            # Sanitize filepath for database insertion
            sanitized_filepath = filepath.replace('\\', '\\\\')  # Escape backslashes
            
            sql = "INSERT INTO uploaded_files (filename, filepath) VALUES (%s, %s)"
            cursor.execute(sql, (filename, sanitized_filepath))
            db.commit()
            cursor.close()
            db.close()
        except MySQLdb.Error as e:
            log_error(f"Database error: {e}")
            os.remove(filepath)  # Remove uploaded file on DB error
            abort(500, description="Failed to save file information to database.")

        return 'File uploaded successfully'

    except Exception as e:
        log_error(f"File upload error: {e}")
        abort(500, description="File upload failed.")

@app.route('/uploads/<filename>')
@requires_auth
def uploaded_file(filename):
    """Serve uploaded files securely."""
    try:
        return send_from_directory(app.config["UPLOAD_FOLDER"], filename)
    except Exception as e:
        log_error(f"File serving error: {e}")
        abort(404, description="File not found")

@app.errorhandler(400)
def bad_request(e):
    """Custom error handler for 400 errors."""
    return f"Bad Request: {e.description}", 400

@app.errorhandler(404)
def not_found(e):
    """Custom error handler for 404 errors."""
    return "File Not Found", 404

@app.errorhandler(500)
def internal_server_error(e):
    """Custom error handler for 500 errors."""
    return "Internal Server Error", 500

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
