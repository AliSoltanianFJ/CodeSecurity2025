from flask import Flask, request, redirect, send_file
import os
import subprocess
import sys
from pathlib import Path

app = Flask(__name__)

# Pre-index allowed images (only specific extensions, immutable mapping)
_IMAGES_DIR = Path(__file__).with_name("images")
_ALLOWED_EXTS = {".png", ".jpg", ".jpeg", ".gif", ".webp"}
_VALID_IMAGES = {
    p.stem: p
    for p in __IMAGES_DIR.glob("*")
    if p.is_file() and p.suffix.lower() in _ALLOWED_EXTS
}

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    image_id = request.args.get("id", "").strip()
    if not image_id:
        return "missing id", 400
    path = _VALID_IMAGES.get(image_id)
    if not path:
        return "not found", 404
    # send_file uses the resolved Path object; no user-controlled path segments
    return send_file(path)
