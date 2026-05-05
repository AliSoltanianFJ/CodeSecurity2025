from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    import re
    # Get the URL parameter and validate its length and characters
    url = request.args.get('url', '').strip()
    if not url or len(url) > 255:
        return "Invalid input.", 400
    if not re.match(r'^[\w\-\.]+$', url):
        return "Invalid URL.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        result = subprocess.run(
            ['ping', '-n', '2', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        if result.returncode == 0:
            return "Ping successful.", 200
        else:
            return "Host unreachable.", 404
    except Exception:
        return "Error occurred.", 500