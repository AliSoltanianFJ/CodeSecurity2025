from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Check if the request contains a PDF file
    p = request.files.get('pdf')
    if not p:
        return "No PDF file provided", 400
    
    # Validate that the file is a PDF
    filename = p.filename
    if not filename.lower().endswith('.pdf'):
        return "Only PDF files are allowed", 400

    # Create uploads directory if it doesn't exist
    upload_dir = os.path.join(os.getcwd(), 'uploads')
    os.makedirs(upload_dir, exist_ok=True)

    # Generate a unique file name to prevent overwriting
    import uuid
    unique_id = uuid.uuid4().hex
    save_path = os.path.join(upload_dir, f"{unique_id}.pdf")

    # Save the file securely
    p.save(save_path)

    return "PDF uploaded successfully", 200

