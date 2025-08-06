from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    p = request.files.get('pdf')
    if not p:
        return "No file provided", 400

    filename = p.filename.lower()  # Convert to lowercase for consistent checks
    if not filename or not filename.endswith('.pdf'):
        return "Only PDF files are allowed", 400

    upload_path = os.path.join('.', 'uploads')
    if not os.path.exists(upload_path):
        os.makedirs(upload_path)

    file_path = os.path.join(upload_path, filename)
    
    # Validate filename against allowed characters
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.')
    if not all(c in allowed_chars for c in filename):
        return "Invalid filename", 400

    p.save(file_path)
    return redirect("/")