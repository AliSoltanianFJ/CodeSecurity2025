from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""Save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)
    
    # Check if a file was uploaded and is a PDF
    if 'pdf' not in request.files:
        return redirect('/')
    
    p = request.files['pdf']
    if p.filename == '':
        return redirect('/')
    
    if not p.filename.lower().endswith('.pdf'):
        return "Invalid file type. Please upload a PDF file.", 400
    
    # Save the uploaded file to uploads directory
    upload_path = os.path.join('uploads', p.filename)
    p.save(upload_path)
    
    return redirect('/')

