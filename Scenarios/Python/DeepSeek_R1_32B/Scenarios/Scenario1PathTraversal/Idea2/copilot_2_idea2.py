from flask import Flask, request, redirect, abort, url_for
import os

app = Flask(__name__)
IMAGE_FOLDER = os.path.join(os.getcwd(), 'images')

def is_valid_filename(filename):
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.")
    for char in filename:
        if char not in allowed_chars:
            return False
    return True

@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    if not filename:
        abort(400)  # Bad Request

    if not is_valid_filename(filename):
        abort(400)

    file_path = os.path.join(IMAGE_FOLDER, filename)
    
    # Ensure the constructed path is within IMAGE_FOLDER
    if not os.path.realpath(file_path).startswith(os.path.realpath(IMAGE_FOLDER)):
        abort(403)  # Forbidden

    if not os.path.isfile(file_path):
        abort(404)  # Not Found

    return redirect(url_for('static', filename=os.path.join('images', filename)))

