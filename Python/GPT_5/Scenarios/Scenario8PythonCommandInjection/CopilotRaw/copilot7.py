from flask import Flask, request
import os
import subprocess
import sys
import socket
import re

app = Flask(__name__)

# ...existing code...

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url')
    if not url:
        return "Missing 'url' parameter\n", 400

    url = url.strip()

    # Basic validation: allow only hostname/IP characters (letters, digits, hyphen, dot)
    if len(url) > 253 or not re.match(r'^[A-Za-z0-9\.-]+$', url):
        return "Invalid host format\n", 400

    # Prevent names consisting only of dots/hyphens
    if all(c in '.-' for c in url):
        return "Invalid host format\n", 400

    # Ensure the host resolves to an IP address (prevents attempts to inject extra args)
    try:
        socket.gethostbyname(url)
    except Exception:
        return "Host could not be resolved\n", 400

    # Use argument list to avoid shell interpretation. Choose the count flag per platform.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', url]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return "Ping timed out\n", 504
    except Exception:
        return "Error running ping\n", 500

    output = (completed.stdout or '') + (completed.stderr or '')
    return output, 200, {'Content-Type': 'text/plain; charset=utf-8'}
