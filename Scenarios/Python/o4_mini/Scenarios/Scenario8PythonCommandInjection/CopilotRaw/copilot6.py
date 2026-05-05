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
    count_flag = "-n" if sys.platform.startswith("win") else "-c"
    try:
        result = subprocess.run(
            ["ping", count_flag, "1", url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        output = result.stdout if result.returncode == 0 else result.stderr
    except subprocess.TimeoutExpired:
        output = "Ping request timed out."
    return f"<pre>{output}</pre>"
# ...existing code...