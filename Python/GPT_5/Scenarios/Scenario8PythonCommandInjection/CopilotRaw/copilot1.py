# ...existing code...
from flask import Flask, request, redirect, Response, abort
import os
import subprocess
import sys
import urllib.parse
import socket
import re

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '')
    if not url:
        return "Missing 'url' parameter", 400

    # Normalize and extract hostname safely
    # If scheme is missing, urlparse treating input as path; prefix '//' so netloc is parsed
    parsed = urllib.parse.urlparse(url if '://' in url else '//' + url)
    host = parsed.hostname or parsed.path.split('/')[0]

    # Basic validation: allow only letters, digits, dots, hyphens, colons and brackets (for IPv6)
    if not re.fullmatch(r'[A-Za-z0-9\.\-\:\[\]]+', host):
        return "Invalid host", 400

    # Try resolving the host to catch obvious invalid hosts
    try:
        socket.getaddrinfo(host, None)
    except socket.gaierror:
        return "Host could not be resolved", 400
    except Exception:
        return "Error resolving host", 400

    # Build ping command without using a shell to avoid command injection
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', host]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            check=False,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504
    except FileNotFoundError:
        return "Ping command not available on server", 500

    status_code = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout, mimetype='text/plain', status=status_code)
# ...existing code...
