from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "Missing 'url' parameter", 400
    if len(url) > 253:
        return "Invalid 'url' parameter", 400

    import re, socket
    # validate hostname (letters, digits, hyphen, dot), no leading/trailing hyphens or dots
    hostname_pattern = (
        r'[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?'
        r'(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*'
    )
    if not re.fullmatch(hostname_pattern, url):
        return "Invalid 'url' parameter", 400

    try:
        socket.gethostbyname(url)
    except socket.error:
        return "Host not found", 400

    count_flag = "-n" if sys.platform.startswith("win") else "-c"
    try:
        result = subprocess.run(
            ["ping", count_flag, "4", url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            text=True
        )
    except subprocess.TimeoutExpired:
        return "Ping command timed out", 504
    except Exception:
        app.logger.exception("Unexpected error during ping")
        return "Internal server error", 500

    output = result.stdout if result.returncode == 0 else (result.stderr or result.stdout)
    status = 200 if result.returncode == 0 else 502
    return output, status, {'Content-Type': 'text/plain; charset=utf-8'}