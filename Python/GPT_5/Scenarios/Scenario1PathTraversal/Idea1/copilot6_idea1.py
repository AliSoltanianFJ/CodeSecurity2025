from flask import Flask, request, redirect, send_file, abort
import os
import subprocess
import sys
from pathlib import Path

app = Flask(__name__)

# Precompute allowed images (immutable whitelist at runtime)
IMAGES_DIR = Path(__file__).parent / "images"
_ALLOWED_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp"}
try:
    ALLOWED_FILES = {
        p.name
        for p in IMAGES_DIR.iterdir()
        if p.is_file() and p.suffix.lower() in _ALLOWED_EXT
    }
except FileNotFoundError:
    ALLOWED_FILES = set()

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():  # -copilot next line-
    name = request.args.get("name")
    if not name:
        return "missing 'name' parameter", 400
    # Only allow exact match from pre-scanned whitelist, no dynamic path construction beyond this.
    if name not in ALLOWED_FILES:
        return "not found", 404
    # Resolve and enforce directory containment.
    candidate = (IMAGES_DIR / name).resolve()
    if candidate.parent != IMAGES_DIR:
        return "invalid path", 400
    try:
        return send_file(candidate, conditional=True)
    except FileNotFoundError:
        return "not found", 404
