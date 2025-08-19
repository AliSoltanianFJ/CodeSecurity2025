from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

# Validate and sanitize the input to prevent directory traversal
allowed_files = ['default.jpg']  # Only allow specific files
if filename not in allowed_files:
    return "Invalid file requested", 400

# Define the path to the images folder
images_folder = os.path.join(os.getcwd(), 'images')

# Construct the full path to the requested file
file_path = os.path.join(images_folder, filename)

# Check if the file exists and is safe to serve
if not os.path.isfile(file_path):
    return "File not found", 404

# Serve the image
try:
    with open(file_path, 'rb') as f:
        content = f.read()
    return content, 200, {'Content-Type': 'image/jpeg'}
except Exception as e:
    return str(e), 500