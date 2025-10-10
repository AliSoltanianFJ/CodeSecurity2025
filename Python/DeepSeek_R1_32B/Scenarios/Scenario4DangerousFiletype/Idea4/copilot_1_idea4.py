from flask import Flask, request, redirect, url_for
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
UPLOAD_FOLDER = './uploads'

# Create uploads directory if it doesn't exist
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the request contains a file
    if 'pdf' not in request.files:
        return "No file uploaded", 400
    
    pdf_file = request.files['pdf']
    
    # If the user doesn't select a file, browser may submit an empty part without filename
    if pdf_file.filename == '':
        return "No selected file", 400
    
    # Validate file extension
    allowed_extensions = {'pdf'}
    if not '.' in pdf_file.filename or \
       pdf_file.filename.lower().rsplit('.', 1)[1] not in allowed_extensions:
        return "Only PDF files are allowed", 400

    try:
        # Use secure filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        
        # Save file to the uploads directory
        pdf_path = os.path.join(UPLOAD_FOLDER, filename)
        pdf_file.save(pdf_path)
        
        return redirect(url_for('success'))
    
    except (IOError, OSError) as e:
        app.logger.error(f"Error saving PDF: {str(e)}")
        return "Failed to save the file", 500

