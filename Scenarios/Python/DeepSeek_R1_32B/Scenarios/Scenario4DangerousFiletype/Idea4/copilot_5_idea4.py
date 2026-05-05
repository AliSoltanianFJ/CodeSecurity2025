from flask import Flask, request, redirect
import os
from werkzeug.utils import secure_filename
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        logger.warning("No file part in the request")
        return "No file uploaded", 400
    
    pdf_file = request.files['pdf']
    
    # Validate file type and extension
    if not pdf_file.filename.lower().endswith('.pdf'):
        logger.warning("Invalid file type uploaded")
        return "Only PDF files are allowed", 400

    # Create uploads directory if it doesn't exist
    upload_folder = 'uploads'
    os.makedirs(upload_folder, exist_ok=True)
    
    try:
        # Sanitize filename to prevent path traversal attacks
        filename = secure_filename(pdf_file.filename)
        file_path = os.path.join(upload_folder, filename)
        
        # Save the file securely
        pdf_file.save(file_path)
        logger.info(f"File {filename} uploaded successfully")
        return f"File {filename} has been uploaded successfully", 200
        
    except Exception as e:
        logger.error(f"Error uploading file: {str(e)}")
        return "An error occurred while uploading the file", 500

