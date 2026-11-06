from flask import Flask, request, abort, send_file, make_response  
import os  
import re  

app = Flask(__name__)  

# 1) Define allowed extensions and a strict filename pattern (alphanumeric + _ or -)
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}  
FILENAME_PATTERN = re.compile(
    r'^[A-Za-z0-9_\-]{1,255}\.(' + '|'.join(ALLOWED_EXTENSIONS) + r')$'
)

@app.route('/img')
def img():
    """
    Serve an image file from the local 'images' directory.
    GET parameter: file=<filename>
    Only filenames matching FILENAME_PATTERN are allowed.
    """
    # 2) Retrieve 'file' parameter safely as a string
    filename = request.args.get('file', '', type=str)
    if not filename:
        # missing parameter
        abort(400)

    # 3) Strictly validate against the whitelisted regex
    if not FILENAME_PATTERN.fullmatch(filename):
        # invalid filename format
        abort(400)

    # 4) Resolve the absolute path of the images directory
    images_dir = os.path.realpath(os.path.join(app.root_path, 'images'))
    if not os.path.isdir(images_dir):
        # prevent misconfiguration from exposing private data
        abort(500)

    # 5) Construct the candidate file path and resolve it
    from werkzeug.utils import secure_filename
    filename = secure_filename(filename)  # sanitize filename
    candidate_path = os.path.realpath(os.path.join(images_dir, filename))

    # 6) Ensure that the resolved path is still under images_dir
    #    This blocks any ../ traversal attempts.
    if not candidate_path.startswith(images_dir + os.sep):
        abort(400)

    # 7) Confirm the file actually exists and is a regular file
    if not os.path.isfile(candidate_path):
        abort(404)

    # 8) Derive the correct MIME type
    ext = filename.rsplit('.', 1)[1].lower()
    mimetype = f'image/{ext}'  # safe because ext is strictly from ALLOWED_EXTENSIONS

    try:
        # 9) Use Flask's send_file with conditional GET support
        response = make_response(
            send_file(
                candidate_path,
                mimetype=mimetype,
                conditional=True  # supports HTTP 304, caching, etc.
            )
        )
        # 10) Security headers to prevent MIME sniffing and XSS
        response.headers['X-Content-Type-Options'] = 'nosniff'
        # (Optional) response.headers['Cache-Control'] = 'public, max-age=3600'
        return response

    except Exception:
        # Do not leak traceback or filesystem layout
        abort(500)
