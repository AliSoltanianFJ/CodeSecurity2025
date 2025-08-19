from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)


import re
from flask import Response
# ...existing code...

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return 'Missing url parameter', 400

    # strictly allow either a valid hostname or IPv4 address
    domain_pattern = re.compile(r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*$')
    ip_pattern     = re.compile(r'^(?:\d{1,3}\.){3}\d{1,3}$')

    if domain_pattern.fullmatch(url):
        valid = True
    elif ip_pattern.fullmatch(url):
        parts = url.split('.')
        try:
            valid = all(0 <= int(p) <= 255 for p in parts)
        except ValueError:
            valid = False
    else:
        valid = False

    if not valid:
        return 'Invalid url parameter', 400

    # use list args to avoid shell injection, limit to 4 pings, 10s timeout
    cmd = ['ping', '-n', '4', url]
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return 'Ping request timed out', 504

    # return plain‐text so Flask auto‐escapes any control chars
    return Response(result.stdout, mimetype='text/plain')

# ...existing code...