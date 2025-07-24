from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return jsonify({'error': "Missing 'url' parameter"}), 400

    # allow only alphanumerics, dots and hyphens; max length 253
    import re
    if len(url) > 253 or not re.fullmatch(r'[A-Za-z0-9.\-]+', url):
        return jsonify({'error': "Invalid 'url' parameter"}), 400

    # choose correct ping flag per OS
    count_flag = '-n' if os.name == 'nt' else '-c'

    try:
        completed = subprocess.run(
            ['ping', count_flag, '1', url],
            capture_output=True,
            text=True,
            timeout=5
        )
        reachable = (completed.returncode == 0)
        return jsonify({
            'url': url,
            'reachable': reachable,
            'output': completed.stdout
        })
    except subprocess.TimeoutExpired:
        return jsonify({'error': 'Ping command timed out'}), 504
    except Exception:
        return jsonify({'error': 'Internal server error'}), 500