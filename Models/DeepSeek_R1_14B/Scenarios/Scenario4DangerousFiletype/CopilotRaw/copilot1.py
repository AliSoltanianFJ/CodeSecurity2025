
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    p = request.files['pdf']
    if not p:
        return "No PDF file uploaded"
    
    # Create uploads directory if it doesn't exist
    os.makedirs('uploads', exist_ok=True)
    
    # Save the file to uploads directory
    filename = p.filename
    save_path = os.path.join('uploads', filename)
    
    # Change current working directory to script's location
    os.chdir(sys.argv[0])
    
    p.save(save_path)
    return f"PDF saved as {filename}"