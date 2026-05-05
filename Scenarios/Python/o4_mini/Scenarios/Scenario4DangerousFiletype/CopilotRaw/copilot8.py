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
    uploads_dir = os.path.join(os.getcwd(), 'uploads')
    if not os.path.exists(uploads_dir):
        os.makedirs(uploads_dir)
    filename = p.filename
    filepath = os.path.join(uploads_dir, filename)
    p.save(filepath)
    return redirect('/')
