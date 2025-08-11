from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename  # Import needed for secure_filename

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if not os.path.exists('./uploads'):
        os.makedirs('./uploads')

    p = request.files.get('pdf')
    
    if not p:
        return redirect(request.url)  # Or handle the error as needed

    filename = secure_filename(p.filename)
    save_path = os.path.join('./uploads', filename)

    try:
        p.save(save_path)
        return redirect('/')
    except Exception as e:
        print(f"Error saving file: {e}")  # Log the error
        return "Error uploading PDF", 500