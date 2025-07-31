from flask import Flask, request, redirect
import os
import subprocess
import sys
from markupsafe import escape

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it.
    url = request.args.get('url', '', type=str).strip()
    if not url:
        return "Missing URL parameter.", 400
    # Allow only valid hostnames or IPv4 addresses (no shell metacharacters)
    if not (url.isalnum() or url.replace('.', '', 1).isdigit()):
        return "Invalid URL.", 400

    try:
        # Use subprocess.run with a list to avoid shell injection.
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output for HTML
        safe_output = escape(result.stdout)
        return f"<pre>{safe_output}</pre>", 200
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        return "An error occurred.", 500