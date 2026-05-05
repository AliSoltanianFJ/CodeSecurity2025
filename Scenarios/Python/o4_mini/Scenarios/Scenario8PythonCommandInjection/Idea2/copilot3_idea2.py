from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re
# ...existing code...
@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return "Missing 'url' parameter", 400
    if len(url) > 255:
        return "URL too long", 400
    if not re.fullmatch(r"[A-Za-z0-9.-]+", url):
        return "Invalid URL", 400
    count_flag = "-n" if os.name == "nt" else "-c"
    try:
        result = subprocess.run(
            ["ping", count_flag, "4", url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return "Ping command timed out", 504
    content = result.stdout if result.returncode == 0 else result.stderr
    status = 200 if result.returncode == 0 else 500
    return content, status, {'Content-Type': 'text/plain; charset=utf-8'}
# ...existing code...