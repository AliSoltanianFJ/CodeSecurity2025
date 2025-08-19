from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from werkzeug.utils import secure_filename

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files or p.filename == '':
        return "No file uploaded", 400
    
    os.makedirs('./uploads', exist_ok=True)
    
    filename = secure_filename(p.filename)
    full_path = os.path.join('./uploads', filename)
    p.save(full_path)
    
    return redirect('/pdf/upload')