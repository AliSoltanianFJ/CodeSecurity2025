from flask import Flask, request, redirect, send_file
import os

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename')
    if not filename:
        return redirect('/')
    
    # Validate the filename to prevent directory traversal
    if os.path.basename(filename) != filename:
        return redirect('/')
    
    img_path = os.path.join('images', filename)
    
    if os.path.exists(img_path):
        return send_file(img_path)
    else:
        return redirect('/404')