from flask import Flask, request, send_from_directory, abort
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

IMAGE_FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        abort(400, "No filename provided")
    
    safe_filename = secure_filename(filename)
    file_path = os.path.join(IMAGE_FOLDER, safe_filename)
    
    if not os.path.isfile(file_path):
        abort(404)
    
    # Only serve from the images directory, do not expose stacktraces
    try:
        return send_from_directory(IMAGE_FOLDER, safe_filename)
    except Exception:
        abort(404)

# Example: /img?file=cat.jpg

if __name__ == "__main__":
    app.run(debug=False)  # Set debug=False to prevent detailed errors in production