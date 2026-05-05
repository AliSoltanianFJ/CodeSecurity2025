from flask import Flask, request, redirect, url_for
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Maximum allowed file size (e.g., 5MB)
MAX_FILE_SIZE = 5 * 1024 * 1024

def validate_pdf(file):
    # Check if the file is a PDF
    if not file.mimetype == 'application/pdf':
        return False, "Invalid MIME type. Only PDF files are allowed."
    
    # Sanitize filename
    filename = os.path.basename(file.filename)
    # Remove any non-alphanumeric characters except dots and underscores
    sanitized_name = ''.join(c for c in filename if c.isalnum() or c in ['.', '_'])
    # Ensure the file has a .pdf extension
    if not sanitized_name.lower().endswith('.pdf'):
        return False, "Invalid filename. Only PDF files are allowed."
    
    return True, sanitized_name

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the request contains a file
    if 'pdf' not in request.files:
        return redirect(url_for('error', message="No file uploaded")), 400
    
    pdf_file = request.files['pdf']
    
    # Check if the file is empty
    if pdf_file.filename == '':
        return redirect(url_for('error', message="Empty filename")), 400
    
    # Validate and sanitize the file
    valid, result = validate_pdf(pdf_file)
    if not valid:
        return redirect(url_for('error', message=result)), 400
    
    sanitized_name = result
    
    try:
        # Check file size before saving
        pdf_file.seek(0, os.SEEK_END)  # Move to end of file
        file_size = pdf_file.tell()
        if file_size > MAX_FILE_SIZE:
            return redirect(url_for('error', message=f"File exceeds maximum allowed size of {MAX_FILE_SIZE/1024/1024:.1f}MB")), 413
        
        # Create uploads directory if it doesn't exist
        uploads_dir = os.path.join(app.root_path, 'uploads')
        if not os.path.exists(uploads_dir):
            os.makedirs(uploads_dir)
        
        # Save the file to the uploads directory with sanitized name
        save_path = os.path.join(uploads_dir, sanitized_name)
        pdf_file.save(save_path)
        
        return redirect(url_for('confirmation'))
    
    except Exception as e:
        # Log the error (recommended to use proper logging instead of print)
        print(f"Error uploading file: {str(e)}", file=sys.stderr)
        return redirect(url_for('error', message=f"An error occurred while uploading the file")), 500

# Example route for confirmation
@app.route('/confirmation')
def confirmation():
    return "File uploaded successfully!"
