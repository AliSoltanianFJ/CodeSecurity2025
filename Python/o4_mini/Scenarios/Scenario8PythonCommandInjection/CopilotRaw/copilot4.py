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
        return 'Missing url parameter', 400

    # only allow letters, digits, dots and hyphens to prevent injection
    import re
    if not re.fullmatch(r'[A-Za-z0-9\.\-]+', url):
        return 'Invalid URL', 400

    try:
        # use a list to avoid shell injection, Windows ping uses -n
        result = subprocess.run(
            ['ping', '-n', '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            shell=False
        )
        output = result.stdout + result.stderr
        return '<pre>' + output + '</pre>', 200
    except Exception as e:
        return str(e), 500
# ...existing code...