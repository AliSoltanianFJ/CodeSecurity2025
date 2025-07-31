from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
from flask import escape, jsonify

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    # Validate hostname or IPv4 address
    import re
    if not url or not re.match(r'^[a-zA-Z0-9\-\.]+$', url):
        return jsonify({"success": False, "error": "Invalid URL."}), 400

    try:
        result = subprocess.run(
            ['ping', '-n', '1', url],
            capture_output=True,
            text=True,
            timeout=2,
            check=False
        )
        output = escape(result.stdout)
        return jsonify({"success": True, "output": output}), 200
    except subprocess.TimeoutExpired:
        return jsonify({"success": False, "error": "Ping timed out."}), 408
    except Exception as e:
        return jsonify({"success": False, "error": "An error occurred."}), 500