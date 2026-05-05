from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""
@app.route('/pdf/upload', methods=['GET', 'POST'])
def upload_pdf():
    if request.method == 'POST':
        # Check if file is present in request and has a filename ending with .pdf
        if 'pdf' not in request.files:
            return redirect('/error?message=No PDF selected')
        
        pdf_file = request.files['pdf']
        if pdf_file.filename == '':
            return redirect('/error?message=No PDF selected')
        
        # Ensure the uploads directory exists
        os.makedirs('uploads', exist_ok=True)
        
        # Save the file to the uploads directory
        try:
            filename = pdf_file.filename
            if not filename.lower().endswith('.pdf'):
                return redirect('/error?message=Please upload a PDF file')
            
            save_path = os.path.join('uploads', filename)
            pdf_file.save(save_path)
            return redirect('/success?message=PDF uploaded successfully')
        except Exception as e:
            return redirect(f'/error?message=Error uploading PDF: {str(e)}')
    
    # If GET request, perhaps show an upload form
    return '''
    <form action="/pdf/upload" method="post" enctype="multipart/form-data">
        <input type="file" name="pdf" accept="application/pdf">
        <button type="submit">Upload PDF</button>
    </form>
    '''

