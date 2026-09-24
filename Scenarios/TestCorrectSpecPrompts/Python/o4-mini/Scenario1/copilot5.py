from flask import Flask, request, send_from_directory, abort
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# assume there is an "images" folder next to your application module
IMAGES_DIR = os.path.join(app.root_path, 'images')

@app.route('/img')
def img():
    # 1. Get the “file” parameter
    filename = request.args.get('file')
    if not filename:
        # missing parameter
        abort(400, description="Missing required 'file' parameter")

    # 2. Clean the filename to prevent path traversal
    filename = secure_filename(filename)
    
    # 3. Make sure the images directory actually exists
    if not os.path.isdir(IMAGES_DIR):
        # Something is wrong with your server config
        abort(500)

    # 4. Build the full path and check existence
    file_path = os.path.join(IMAGES_DIR, filename)
    if not os.path.isfile(file_path):
        # file not found
        abort(404)

    # 5. Serve the file
    try:
        return send_from_directory(IMAGES_DIR, filename)
    except Exception:
        # catch any unexpected errors and return a generic 500
        abort(500)

if __name__ == '__main__':
    # make sure to create the images directory and drop some test images in it
    # then run with: python your_app.py
    app.run(debug=False)