from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    # Sanitize input to avoid command injection
    # Only allow alphanumeric, dots, and hyphens
    import re
    if not re.match(r'^[a-zA-Z0-9.-]+$', url):
        return "Invalid URL format.", 400

    try:
        # Use subprocess.run for safety, pass arguments as a list
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5
        )
        output = result.stdout
        return f"<pre>{output}</pre>"
    except Exception as e:
        return f"Error: {str(e)}", 500
# ...existing code...