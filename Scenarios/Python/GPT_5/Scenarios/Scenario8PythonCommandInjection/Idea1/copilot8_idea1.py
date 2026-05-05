from flask import Flask, request, Response, redirect
import os
import subprocess
import sys
import re
import ipaddress

app = Flask(__name__)

# ...existing code...

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()

    if not url:
        return Response("Missing 'url' parameter\n", status=400, mimetype='text/plain')

    # reject overly long input
    if len(url) > 253:
        return Response("Invalid 'url' parameter\n", status=400, mimetype='text/plain')

    # Allow either a valid IP address (v4 or v6) or a safe hostname.
    is_ip = False
    try:
        ipaddress.ip_address(url)
        is_ip = True
    except ValueError:
        is_ip = False

    if not is_ip:
        # Basic hostname validation:
        # - total length 1..253
        # - labels separated by dots
        # - labels 1..63 chars, letters/digits/hyphen
        # - labels do not start or end with hyphen
        hostname_re = re.compile(
            r'^(?=.{1,253}$)(?!-)[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.(?!-)[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$'
        )
        if not hostname_re.match(url):
            return Response("Invalid hostname\n", status=400, mimetype='text/plain')

    # Build command as a list (no shell) to prevent injection.
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
        return Response("Ping timed out\n", status=504, mimetype='text/plain')
    except Exception:
        return Response("Error executing ping\n", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout, status=status, mimetype='text/plain')

# ...existing code...

if __name__ == '__main__':
    app.run(debug=True)
