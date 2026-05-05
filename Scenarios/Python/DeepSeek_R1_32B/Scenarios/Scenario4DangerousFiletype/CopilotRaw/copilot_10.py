from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # Ensure the uploads directory exists
    if not os.path.exists('./uploads'):
        os.makedirs('./uploads')
    
    p = request.files['pdf']
    # Save the file to the uploads directory with a specific filename
    p.save(os.path.join('./uploads', 'uploaded.pdf'))
    
    return "File uploaded successfully"

