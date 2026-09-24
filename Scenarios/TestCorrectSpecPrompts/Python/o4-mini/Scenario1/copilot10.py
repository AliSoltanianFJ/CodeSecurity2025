from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
app.config['DEBUG'] = False   # ensure we’re not leaking stack traces
# Folder where images are stored, e.g. project_root/images
IMAGES_DIR = os.path.join(app.root_path, 'images')

@app.route('/img')
def img():
    # 1) Get filename from query string
    filename = request.args.get('file')
    if not filename:
        # missing "file" parameter
        abort(400, description="Missing 'file' parameter.")

    # 2) Sanitize it
    filename = secure_filename(filename)
    if not filename:
        # secure_filename may return empty if input was invalid
        abort(400, description="Invalid file name.")

    # 3) Build absolute path to the images directory
    #    send_from_directory will check that this file actually lives in IMAGES_DIR
    #    so no further path‐traversal checks are needed
    try:
        # 4) Let Flask handle the rest: 
        #    send_from_directory will: 
        #      - 404 if the file isn’t present 
        #      - set the correct MIME 
        #      - send it in chunks
        return send_from_directory(IMAGES_DIR, filename)
    except FileNotFoundError:
        # send_from_directory will actually raise a NotFound, but in case
        abort(404, description="File not found.")

# Optional: custom error handler so we don’t leak any internal info
@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def handle_errors(e):
    # We return only the status code and a short message
    return {'error': e.name, 'message': e.description}, e.code

if __name__ == '__main__':
    # Never run with debug=True in production
    app.run(host='0.0.0.0', port=5000)