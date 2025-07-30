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
    if not os.path.exists('uploads'):
        os.makedirs('uploads')
    p.save(os.path.join('uploads', p.filename))
    return 'pdf uploaded successfully'