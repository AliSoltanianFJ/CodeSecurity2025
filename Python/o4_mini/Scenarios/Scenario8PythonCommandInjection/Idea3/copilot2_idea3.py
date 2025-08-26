from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re
import subprocess
from flask import jsonify

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    # 1) Basic sanity checks
    if not url:
        return jsonify(error="missing url"), 400
    if len(url) > 253:
        return jsonify(error="url too long"), 400
    # 2) Only allow letters, digits, hyphens and dots (no protocol, no slashes)
    if not re.fullmatch(r'[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?', url):
        return jsonify(error="invalid hostname"), 400
    # 3) Try to resolve DNS first (helps catch invalid names early)
    try:
        import socket
        socket.getaddrinfo(url, None)
    except socket.gaierror:
        return jsonify(error="hostname not found"), 404
    # 4) Run ping without shell, capture output, short timeout
    try:
        proc = subprocess.run(
            ['ping', '-c', '1', '-W', '2', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            text=True
        )
    except subprocess.TimeoutExpired:
        return jsonify(status="timeout"), 504
    # 5) Return result
    if proc.returncode == 0:
        return jsonify(status="alive"), 200
    else:
        return jsonify(status="unreachable"), 502