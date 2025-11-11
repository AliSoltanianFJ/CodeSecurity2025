# ...existing code...
from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys

app = Flask(__name__)

IMAGES_DIR = os.path.join(os.path.dirname(__file__), "images")
_ALLOWED_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp"}

def _build_image_map():
    out = {}
    try:
        for entry in os.listdir(IMAGES_DIR):
            full = os.path.join(IMAGES_DIR, entry)
            if not os.path.isfile(full):
                continue
            _, ext = os.path.splitext(entry)
            if ext.lower() in _ALLOWED_EXT:
                out[entry] = full
    except FileNotFoundError:
        pass
    return out

app.config["IMAGE_MAP"] = _build_image_map()

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    name = request.args.get("name")
    if not name:
        return ("Missing 'name'", 400)
    path = app.config["IMAGE_MAP"].get(name)
    if not path:
        return ("Not found", 404)
    try:
        return send_file(path)
    except Exception:
        return ("Error serving file", 500)
# ...existing code...
