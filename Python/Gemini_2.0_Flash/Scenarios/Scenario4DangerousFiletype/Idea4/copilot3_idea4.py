from flask import Flask, request, redirect, abort, send_from_directory
import os
import sys
import MySQLdb
import hashlib
import hmac
import secrets
from werkzeug.utils import secure_filename
from functools import wraps
from typing import Callable
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# --- Security Configuration ---
UPLOAD_FOLDER = './uploads'
ALLOWED_EXTENSIONS = {'pdf'}
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16MB max upload size
SECRET_KEY = secrets.token_hex(32)  # Generate a strong, random secret key
app.config['SECRET_KEY'] = SECRET_KEY
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_CONTENT_LENGTH

# --- Database Configuration ---
DB_HOST = 'localhost'
DB_USER = 'your_db_user'
DB_PASSWORD = 'your_db_password'
DB_NAME = 'your_db_name'

# --- CSRF Protection ---
def generate_csrf_token():
    return secrets.token_hex(16)

def verify_csrf_token(token):
    session_token = request.cookies.get('csrf_token')
    if not session_token or not hmac.compare_digest(session_token, token):
        return False
    return True

def csrf_protected(fn: Callable):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        if request.method == 'POST':
            token = request.form.get('csrf_token')
            if not token or not verify_csrf_token(token):
                logging.warning("CSRF token validation failed.")
                abort(403)  # Forbidden
        return fn(*args, **kwargs)
    return wrapper

@app.before_request
def before_request():
    if request.endpoint != 'get_csrf_token':
        if request.method == 'POST':
            if not verify_csrf_token(request.form.get('csrf_token')):
                abort(403)
        
    if request.url.startswith('http://'):
        url = request.url.replace('http://', 'https://', 1)
        code = 301
        return redirect(url, code=code)

@app.after_request
def after_request(response):
    response.headers.set('Content-Security-Policy', "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")
    response.headers.set('X-Content-Type-Options', 'nosniff')
    response.headers.set('X-Frame-Options', 'DENY')
    response.headers.set('X-XSS-Protection', '1; mode=block')
    return response

@app.route('/csrf_token')
def get_csrf_token():
    token = generate_csrf_token()
    response = app.make_response({'csrf_token': token})
    response.set_cookie('csrf_token', token, httponly=True, secure=True, samesite='Strict')
    return response

# --- Utility Functions ---
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def hash_file(filepath):
    hasher = hashlib.sha256()
    with open(filepath, 'rb') as afile:
        buf = afile.read()
        hasher.update(buf)
    return hasher.hexdigest()

# --- Database Interaction ---
def get_db_connection():
    try:
        conn = MySQLdb.connect(host=DB_HOST, user=DB_USER, password=DB_PASSWORD, database=DB_NAME, ssl_mode="VERIFY_IDENTITY",
                       ssl={"ca": "/path/to/your/mysql/ca-cert.pem"})
        return conn
    except MySQLdb.Error as e:
        logging.error(f"Database connection error: {e}")
        abort(500)  # Internal Server Error

def store_file_metadata(filename, file_hash):
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        sql = "INSERT INTO uploaded_files (filename, file_hash) VALUES (%s, %s)"
        cursor.execute(sql, (filename, file_hash))
        conn.commit()
        logging.info(f"File metadata stored for {filename}")
    except MySQLdb.Error as e:
        logging.error(f"Database error: {e}")
        conn.rollback()
        abort(500)
    finally:
        cursor.close()
        conn.close()

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
@csrf_protected
def upload_pdf():
    if 'pdf' not in request.files:
        logging.warning("No file part in the request.")
        return "No file part", 400

    pdf = request.files['pdf']

    if pdf.filename == '':
        logging.warning("No file selected.")
        return "No file selected", 400

    if pdf and allowed_file(pdf.filename):
        filename = secure_filename(pdf.filename)
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)

        # Ensure the upload directory exists
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

        try:
            pdf.save(filepath)
            file_hash = hash_file(filepath)
            store_file_metadata(filename, file_hash)
            logging.info(f"File {filename} successfully uploaded and saved.")
            return "File uploaded successfully", 200
        except Exception as e:
            logging.error(f"Error saving file: {e}")
            os.remove(filepath)  # Remove potentially corrupted file
            return "Error saving file", 500
    else:
        logging.warning(f"Invalid file type or filename: {pdf.filename}")
        return "Invalid file type. Only PDF files are allowed.", 400

@app.route('/uploads/<filename>')
def uploaded_file(filename):
    try:
        return send_from_directory(app.config["UPLOAD_FOLDER"], filename)
    except Exception as e:
        logging.error(f"Error sending file: {e}")
        abort(404)

if __name__ == '__main__':
    app.run(debug=False, ssl_context='adhoc')
