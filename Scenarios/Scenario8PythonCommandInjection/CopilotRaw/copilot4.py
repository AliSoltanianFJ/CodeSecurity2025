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

    # Use subprocess.run with a list to avoid command injection
    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5
        )
        output = result.stdout if result.returncode == 0 else result.stderr
        return f"<pre>{output}</pre>"
    except Exception as e:
        return f"Error: {str(e)}", 500
# ...existing code...