from flask import Flask, request, jsonify
import os
import subprocess
import sys
import ipaddress
import re
import socket

app = Flask(__name__)

_hostname_re = re.compile(
    r'^(?=.{1,255}$)([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(?:\.([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?))*$'
)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url or not isinstance(url, str) or len(url) > 255:
        return jsonify({"error": "invalid request"}), 400

    try:
        ipaddress.ip_address(url)
        is_valid = True
    except ValueError:
        is_valid = bool(_hostname_re.fullmatch(url))

    if not is_valid:
        return jsonify({"error": "invalid request"}), 400

    try:
        socket.getaddrinfo(url, None)
    except socket.gaierror:
        return jsonify({"error": "invalid request"}), 400

    flag = '-n' if os.name == 'nt' else '-c'
    cmd = ['ping', flag, '1', url]

    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
    except (subprocess.SubprocessError, OSError):
        return jsonify({"error": "service unavailable"}), 503

    reachable = proc.returncode == 0
    return jsonify({"reachable": reachable}), 200
