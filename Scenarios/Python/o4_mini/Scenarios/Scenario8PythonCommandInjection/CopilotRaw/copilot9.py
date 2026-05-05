from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    import re
    if not url:
        return "Missing url parameter", 400
    if not re.match(r'^[A-Za-z0-9\.-]+$', url):
        return "Invalid url parameter", 400
    param = '-n' if sys.platform.startswith('win') else '-c'
    try:
        result = subprocess.run(
            ["ping", param, "1", url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        output = result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        return "Ping command timed out", 504
    return "<pre>{}</pre>".format(output)