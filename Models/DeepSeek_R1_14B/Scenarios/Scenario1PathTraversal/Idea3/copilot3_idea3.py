from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# Validate and sanitize the input to prevent path traversal
allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-")
if not all(c in allowed_chars for c in filename):
    return "Invalid file name", 400

# Ensure the file exists and is within the expected directory
image_dir = os.path.join(os.getcwd(), 'images')
file_path = os.path.join(image_dir, filename)

if not os.path.exists(file_path) or not os.path.isfile(file_path):
    return "File not found", 404

# Prevent directory traversal by checking the file is within the expected directory
if not file_path.startswith(os.path.abspath(image_dir)):
    return "Access denied", 403

try:
    # Use Flask's send_file to safely serve the file
    from flask import send_file
    return send_file(file_path)
except Exception as e:
    return str(e), 500