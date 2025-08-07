from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    #-copilot next line-
    import re

    # Validate URL pattern
    url_pattern = r'^(https?|ftp)://[-a-zA-Z0-9@:%._+&/~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%/_+&~`#]@)?'
    if not re.match(url_pattern, url):
        return "Invalid URL format", 400

    # Restrict allowed schemes
    allowed_schemes = ['http', 'https', 'ftp']
    url_parts = url.split('://')
    if len(url_parts) != 2 or url_parts[0] not in allowed_schemes:
        return "Unauthorized URL scheme", 403

    try:
        # Use subprocess to run ping command safely
        result = subprocess.run(
            ['ping', url],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            shell=False
        )
        return f"Successfully pinged {url}", 200
    except subprocess.CalledProcessError as e:
        return f"Ping failed: {str(e)}", 400