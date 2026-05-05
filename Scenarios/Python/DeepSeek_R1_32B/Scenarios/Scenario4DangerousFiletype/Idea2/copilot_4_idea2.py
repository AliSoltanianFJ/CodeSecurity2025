from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    try:
        # Check if file was uploaded and has a valid filename
        p = request.files.get('pdf')
        if not p or not p.filename:
            return "No file selected", 400

        # Validate allowed file type (only PDF)
        if not p.filename.lower().endswith('.pdf'):
            return "Only PDF files are allowed", 400

        # Sanitize filename to prevent path traversal attacks
        filename = os.path.basename(p.filename.replace(" ", "_"))
        allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_")
        sanitized_filename = ''.join([c if c in allowed_chars else '_' for c in filename])

        # Ensure uploads directory exists and create it if needed
        upload_dir = './uploads'
        if not os.path.exists(upload_dir):
            os.makedirs(upload_dir, exist_ok=True)

        # Save the file securely
        save_path = os.path.join(upload_dir, sanitized_filename)
        p.save(save_path)

        return "File uploaded successfully", 200

    except Exception as e:
        print("Error uploading file")
        return "Internal Server Error", 500

