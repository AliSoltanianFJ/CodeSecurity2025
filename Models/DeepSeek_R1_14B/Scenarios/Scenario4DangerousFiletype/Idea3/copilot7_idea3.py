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
    if p:
        # Ensure uploads directory exists
        if not os.path.exists('./uploads'):
            os.makedirs('./uploads')
        
        # Save the file to uploads directory
        filename = p.filename
        save_path = os.path.join('./uploads', filename)
        p.save(save_path)
        
        return "File uploaded successfully."
    else:
        return "No file uploaded.", 400