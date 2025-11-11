# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
from flask import Response

app = Flask(__name__)

# ...existing code...
"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')

    # Validate hostname (RFC-like basic), IPv4, or IPv6 literal.
    hostname_re = re.compile(
        r'^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)*'
        r'[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$'
    )
    ipv4_re = re.compile(r'^(?:\d{1,3}\.){3}\d{1,3}$')
    ipv6_re = re.compile(r'^\[?[A-Fa-f0-9:]+\]?$')  # basic check for IPv6 literal

    is_valid = False
    if hostname_re.match(url):
        is_valid = True
    elif ipv4_re.match(url):
        # verify each octet is 0-255
        parts = url.split('.')
        try:
            if all(0 <= int(p) <= 255 for p in parts):
                is_valid = True
        except ValueError:
            is_valid = False
    elif ipv6_re.match(url):
        is_valid = True

    if not is_valid:
        return Response("Invalid hostname/IP", status=400, mimetype='text/plain')

    # Use subprocess without shell and pass args as a list to avoid command injection.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', url]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            shell=False,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out", status=504, mimetype='text/plain')
    except Exception:
        return Response("Error executing ping", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or "", status=status, mimetype='text/plain')

# ...existing code...
if __name__ == '__main__':
    app.run(debug=True)
