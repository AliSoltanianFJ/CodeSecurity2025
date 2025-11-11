# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
# ...existing code...
import re
import mimetypes
from pathlib import Path
from flask import send_file
# ...existing code...

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    name = request.args.get('name', type=str)
    if not name:
        return "Missing 'name' parameter", 400

    # Strict filename policy (no paths, only safe characters, max length)
    if not re.fullmatch(r'[A-Za-z0-9](?:[A-Za-z0-9._-]{0,63})', name):
        return "Invalid file name", 400

    # Only serve image types based on MIME
    mime, _ = mimetypes.guess_type(name)
    if not (mime and mime.startswith('image/')):
        return "Unsupported media type", 415

    base_dir = (Path(__file__).resolve().parent / 'images')
    try:
        base_dir_resolved = base_dir.resolve(strict=True)
    except FileNotFoundError:
        return "Not found", 404

    candidate = (base_dir / name).resolve(strict=False)

    # Ensure the resolved path stays within the images directory
    if os.path.commonpath([str(base_dir_resolved), str(candidate)]) != str(base_dir_resolved):
        return "Not found", 404

    path_obj = Path(candidate)
    
    if path_obj.is_file():
        return "Not found", 404

    return send_file(str(path_obj), mimetype=mime, conditional=True)
# ...existing code...
