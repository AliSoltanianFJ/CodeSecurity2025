from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if it's a POST request with files
    if request.method == 'POST':
        p = request.files.get('pdf')
        
        # Check if file was uploaded
        if not p:
            return "No PDF file uploaded.", 400
        
        # Validate the file is a PDF based on MIME type and extension
        if p.mimetype != 'application/pdf' or not p.filename.lower().endswith('.pdf'):
            return "Only PDF files are allowed.", 400
        
        # Sanitize the filename to prevent path traversal attacks
        filename = os.path.basename(p.filename)
        
        # Ensure the uploads directory exists
        upload_dir = os.path.join(os.getcwd(), "uploads")
        if not os.path.exists(upload_dir):
            os.makedirs(upload_dir, exist_ok=True)  # Using exist_ok=True for race condition prevention
        
        # Save the file to the uploads directory
        try:
            file_path = os.path.join(upload_dir, filename)
            p.save(file_path)
            return redirect("/")
        except Exception as e:
            return f"Error saving file: {str(e)}", 500
    
    return "Method not allowed.", 405

