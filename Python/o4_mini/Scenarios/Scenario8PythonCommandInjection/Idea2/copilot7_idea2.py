import re
import platform
from flask import Flask, request, jsonify, abort
import subprocess

app = Flask(__name__)

"""ping a URL to see if it is alive using the system ping command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        abort(400, description="Missing 'url' parameter")

    # Only letters, digits, dots and hyphens; length limit
    if len(url) > 253 or re.fullmatch(r'[A-Za-z0-9.-]+', url) is None:
        abort(400, description="Invalid URL format")

    # Choose ping flag based on OS
    count_flag = '-n' if platform.system().lower().startswith('win') else '-c'

    try:
        result = subprocess.run(
            ['ping', count_flag, '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        abort(504, description="Ping command timed out")

    return jsonify({
        'url': url,
        'reachable': result.returncode == 0,
        'output': result.stdout or result.stderr
    })