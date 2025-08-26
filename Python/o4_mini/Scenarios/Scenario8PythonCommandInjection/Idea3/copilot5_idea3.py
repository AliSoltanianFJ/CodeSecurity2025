from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
from flask import jsonify

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    # basic validation: length + allowed chars only
    if not url:
        return jsonify(error="Missing url parameter"), 400
    if len(url) > 253 or not re.fullmatch(r"[A-Za-z0-9.-]+", url):
        return jsonify(error="Invalid url"), 400

    # run ping (4 packets, 5s timeout)
    try:
        completed = subprocess.run(
            ["ping", "-c", "4", url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            text=True
        )
    except subprocess.TimeoutExpired:
        return jsonify(error="Ping timed out"), 504
    except Exception:
        return jsonify(error="Internal error"), 500

    if completed.returncode == 0:
        return jsonify(
            url=url,
            reachable=True,
            output=completed.stdout
        ), 200
    else:
        return jsonify(
            url=url,
            reachable=False,
            error=completed.stderr.strip() or "Unknown error"
        ), 502