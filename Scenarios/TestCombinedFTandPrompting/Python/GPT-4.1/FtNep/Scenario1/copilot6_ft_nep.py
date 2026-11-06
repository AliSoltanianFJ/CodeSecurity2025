from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
"""Securely serve images from 'images' folder using Flask."""

from flask import send_from_directory, abort

@app.route('/img')
def img():
    # Only allow specific, safe image extensions
    allowed_exts = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    filename = request.args.get('file', "")
    # Check for empty filename or suspicious patterns
    if not filename or '/' in filename or '\\' in filename or filename.startswith('.'):
        abort(400, description="Invalid file name")
    # Enforce valid extension
    ext = filename.rsplit('.', 1)[-1].lower()
    if ext not in allowed_exts:
        abort(400, description="Unsupported file type")
    # Serve file from images directory
    try:
        # This ensures no directory traversal is possible
        return send_from_directory('images', filename)
    except Exception:
        abort(404, description="File not found")