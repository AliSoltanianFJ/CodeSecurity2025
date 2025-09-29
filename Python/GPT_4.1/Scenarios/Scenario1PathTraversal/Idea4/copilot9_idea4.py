from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# ...existing code...

@app.route('/img')
def img():
    # SECURITY: Only allow serving files from the 'images' directory.
    # Prevent path traversal by strictly validating the filename.
    from flask import send_from_directory, abort

    # Get filename from query parameter
    filename = request.args.get('file', '')

    # SECURITY: Only allow filenames that match a strict whitelist pattern (alphanumeric, dash, underscore, dot, and jpg/png extensions)
    import re
    if not re.fullmatch(r'[A-Za-z0-9_\-\.]+\.(jpg|jpeg|png|gif)$', filename):
        # SECURITY: Do not leak details about invalid input
        abort(400)

    # SECURITY: Use a fixed directory, never user input
    images_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'images')

    # SECURITY: Principle of least privilege: only serve files, never execute or write
    try:
        # send_from_directory prevents path traversal and sets safe headers
        return send_from_directory(images_dir, filename, as_attachment=False)
    except FileNotFoundError:
        # SECURITY: Do not leak filesystem details
        abort(404)
    except Exception:
        # SECURITY: Generic error to avoid leaking sensitive info
        abort(500)

# ...existing code...