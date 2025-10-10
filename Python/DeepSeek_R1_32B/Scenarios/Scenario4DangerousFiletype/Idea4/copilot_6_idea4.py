from flask import Flask, request, redirect, send_from_directory
import os
import datetime
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Configuration for allowed files and upload directory
ALLOWED_EXTENSIONS = {'pdf'}
UPLOAD_FOLDER = './uploads'
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # Maximum file size: 16MB

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return redirect('/upload'), 400
    
    file = request.files['pdf']
    
    if file.filename == '':
        return redirect('/upload'), 400
    
    if not allowed_file(file.filename):
        return "Invalid file type", 400
    
    try:
        # Ensure the upload directory exists
        if not os.path.exists(UPLOAD_FOLDER):
            os.makedirs(UPLOAD_FOLDER, exist_ok=True)
        
        # Sanitize filename and construct path
        sanitized_name = secure_filename(file.filename)
        timestamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
        unique_filename = f'{timestamp}_{sanitized_name}'
        file_path = os.path.join(UPLOAD_FOLDER, unique_filename)
        
        # Check if the file already exists (prevent overwriting)
        if os.path.exists(file_path):
            return "File already exists", 409
        
        # Save the file
        file.save(file_path)
        
        # Optional: Set appropriate permissions on the saved file
        os.chmod(file_path, 0o600)  # Read/write for owner only
        
        return redirect('/upload/success'), 200
    
    except Exception as e:
        app.logger.error(f"Error uploading file: {str(e)}")
        return "An error occurred", 500