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

# Custom error handler for file size limit
@app.errorhandler(413)
def request_entity_too_large(e):
    logging.error("Request Entity Too Large: %s", e)
    return "File size exceeds the limit.", 413

# Security Headers
@app.after_request
def add_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
    return response

# Authentication decorator
def authenticate():
    """Authenticates with HTTP Basic Auth."""
    message = {'message': "Authentication Required"}
    resp = flask.Response(json.dumps(message), 401, {'WWW-Authenticate': 'Basic realm="Login Required"'})
    return resp

def requires_auth(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not verify_credentials(auth.username, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def verify_credentials(username, password):
    # Use a secure method to verify credentials (e.g., bcrypt)
    # Replace with your actual authentication logic
    # This is a placeholder and MUST be replaced with secure authentication
    if username == 'admin' and password == 'securepassword':
        return True
    return False

# CSRF protection
def generate_csrf_token():
    return secrets.token_hex(16)

@app.before_request
def csrf_protect():
    if request.method == "POST":
        token = session.pop('_csrf_token', None)
        if not token or token != request.form.get('_csrf_token'):
            abort(403)

def get_csrf_token():
    token = secrets.token_hex(16)
    session['_csrf_token'] = token
    return token

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
@requires_auth
def upload_pdf():
    if 'pdf' not in request.files:
        logging.warning("No PDF file provided in request.")
        return "No file part", 400
    
    pdf = request.files['pdf']
    
    if pdf.filename == '':
        logging.warning("No file selected.")
        return "No selected file", 400

    if pdf and allowed_file(pdf.filename):
        filename = secure_filename(pdf.filename)
        
        # Generate a unique, unpredictable filename
        unique_filename = generate_unique_filename(filename)
        
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
        
        try:
            pdf.save(file_path)
            logging.info(f"File saved successfully to {file_path}")
            return "File uploaded successfully", 200
        except Exception as e:
            logging.error(f"Error saving file: {e}")
            return "Error uploading file", 500
    else:
        logging.warning(f"Invalid file type or filename.")
        return "Invalid file type or filename", 400

# Serve files securely
@app.route('/uploads/<filename>')
@requires_auth
def serve_file(filename):
    try:
        if not is_safe_path(UPLOAD_FOLDER, filename):
            logging.warning(f"Attempted access to unsafe path.")
            abort(400)
        return send_from_directory(UPLOAD_FOLDER, filename)
    except Exception as e:
        logging.error(f"Error serving file: {e}")
        abort(404)

# Validate filename
def allowed_file(filename):
    ALLOWED_EXTENSIONS = {'pdf'}
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# Generate unique filename
def generate_unique_filename(filename):
    timestamp = str(int(datetime.datetime.now().timestamp()))
    hash_object = hashlib.sha256((filename + timestamp + app.config['SECRET_KEY']).encode('utf-8'))
    hash_filename = hash_object.hexdigest()
    _, ext = os.path.splitext(filename)
    return hash_filename + ext

# Secure path check to prevent directory traversal
def is_safe_path(basedir, filename):
    file_path = os.path.join(basedir, filename)
    try:
        file_path = os.path.realpath(file_path)
        base_path = os.path.realpath(basedir)
        return base_path == os.path.commonpath((base_path, file_path))
    except ValueError:
        return False

if __name__ == '__main__':
    app.run(debug=False, host='0.0.0.0', port=5000)
