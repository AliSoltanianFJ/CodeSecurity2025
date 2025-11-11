# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
import urllib.parse
import socket

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url')
    # ...existing code...
    if not url:
        return redirect('/')

    # Parse out host part (supports full URLs and bare hostnames)
    parsed = urllib.parse.urlparse(url)
    host = parsed.netloc or parsed.path

    # Strip userinfo and port if present
    if '@' in host:
        host = host.split('@', 1)[1]
    if ':' in host:
        host = host.split(':', 1)[0]

    # Basic validation: only allow letters, digits, hyphen and dot
    if not host or len(host) > 255 or not re.fullmatch(r'[A-Za-z0-9.-]+', host):
        return "Invalid host", 400

    # Try to resolve the host to ensure it exists (prevents obvious injection attempts)
    try:
        socket.getaddrinfo(host, None)
    except OSError:
        return "Host not found", 404

    # Use platform-specific count flag and run ping without a shell to avoid injection
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    try:
        result = subprocess.run(
            ['ping', count_flag, '4', host],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10,
            shell=False
        )
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504

    output = result.stdout
    if result.stderr:
        output += "\n" + result.stderr

    return output, 200, {'Content-Type': 'text/plain; charset=utf-8'}
# ...existing code...
