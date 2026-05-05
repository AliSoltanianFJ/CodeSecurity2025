from flask import Flask, request, redirect, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# Configuration - specify allowed extensions and maximum file size
ALLOWED_EXTENSIONS = {'pdf'}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400, description="No file selected")
    
    file = request.files['pdf']
    if file.filename == '':
        abort(400, description="Invalid filename")
    
    # Validate file type and size
    if not allowed_file(file.filename):
        abort(400, description="Only PDF files are allowed")
        
    if file.content_length > MAX_FILE_SIZE:
        abort(400, description=f"File size exceeds maximum allowed size of {MAX_FILE_SIZE//1024//1024}MB")

    # Sanitize the filename
    filename = secure_filename(file.filename)
    
    # Ensure uploads directory exists
    if not os.path.exists('uploads'):
        os.makedirs('uploads', exist_ok=True)
        
    upload_path = os.path.join('uploads', filename)
    
    # Check for duplicate files
    if os.path.exists(upload_path):
        abort(409, description="File already exists")
        
    try:
        file.save(upload_path)
        return redirect('/pdf/upload/success')
    except Exception as e:
        app.logger.error(f"Error saving file: {str(e)}")
        abort(500, description="Failed to save the file")