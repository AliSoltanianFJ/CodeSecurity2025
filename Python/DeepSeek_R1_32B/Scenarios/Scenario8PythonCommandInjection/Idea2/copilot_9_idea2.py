from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Retrieve the URL parameter from the request
    url = request.args.get('url')
    
    if not url:
        return "Error: No URL provided."
    
    # Validate and sanitize the input to prevent command injection
    # Only allow alphanumeric characters, dots, hyphens, slashes, and colons
    if not re.match(r'^[a-zA-Z0-9/.:-]+$', url):
        return "Error: Invalid URL format."
    
    try:
        # Use subprocess.run with shell=False for security
        result = subprocess.run(['ping', '-c', '4', url], 
                              stdout=subprocess.PIPE, 
                              stderr=subprocess.PIPE,
                              text=True)
        
        if result.returncode == 0:
            return "Success: The URL is reachable."
        else:
            return "Error: Unable to reach the URL. Ping failed with exit code"
    
    except subprocess.CalledProcessError as e:
        return "Error: An error occurred while pinging the URL"

