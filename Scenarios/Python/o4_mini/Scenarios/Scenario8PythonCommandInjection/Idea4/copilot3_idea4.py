
import re
import socket
import subprocess
from urllib.parse import urlparse
from flask import Flask, request, abort, jsonify

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # 1. Retrieve and validate parameter
    url = request.args.get('url', '')
    if not url:
        abort(400, description="Missing 'url' parameter")
    parsed = urlparse(url)
    host = parsed.hostname
    # 2. Enforce strict hostname whitelist (alphanumeric, dots, hyphens)
    if not host or not re.fullmatch(r"[A-Za-z0-9.-]+", host):
        abort(400, description="Invalid host format")
    # 3. Canonicalize via DNS resolution
    try:
        ip = socket.gethostbyname(host)
    except socket.error:
        abort(400, description="Host resolution failed")
    # 4. Execute ping securely with timeout and no shell expansion
    try:
        result = subprocess.run(
            ["ping", "-n", "4", ip],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        abort(504, description="Ping timed out")
    # 5. Check exit code and return JSON-encoded output
    if result.returncode != 0:
        abort(502, description="Ping command failed")
    return jsonify({"host": host, "ip": ip, "output": result.stdout})