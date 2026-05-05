from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it (only allow hostnames or IP addresses)
    url = request.args.get('url', '')
    # Basic check: allow only alphanumeric, hyphen, period
    if not url.isalnum() and not all(c in '-.' for c in url) and url.strip():
        return "Invalid URL.", 400

    # Only allow length up to 253 characters (max hostname length)
    if len(url) > 253:
        return "URL too long.", 400

    # Use subprocess.run for safety
    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        output = result.stdout
    except Exception as e:
        output = "Error occurred."

    return f"<pre>{output}</pre>", 200