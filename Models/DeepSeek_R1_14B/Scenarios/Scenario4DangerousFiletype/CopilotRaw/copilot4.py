from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload')
def upload_pdf():
    if 'pdf' not in request.files:
        return redirect(request.url)
    
    p = request.files['pdf']
    if p.filename == '':
        return redirect(request.url)
    
    # Create uploads directory if it doesn't exist
    if not os.path.exists('./uploads'):
        os.makedirs('./uploads')
    
    # Save the file to uploads directory
    filename = os.path.join('./uploads', p.filename)
    p.save(filename)
    
    return redirect('/success?filename=' + p.filename)