from flask import Flask, request, redirect, jsonify, escape
import os
import subprocess
import sys
# ...existing code...

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return jsonify(error='Missing url parameter'), 400

    import re
    hostname_pattern = re.compile(
        r'^(?:\d{1,3}\.){3}\d{1,3}$|^(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,}$'
    )
    if not hostname_pattern.fullmatch(url):
        return jsonify(error='Invalid url parameter'), 400

    # choose ping count flag based on platform
    count_flag = '-n' if os.name == 'nt' else '-c'

    try:
        result = subprocess.run(
            ['ping', count_flag, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            text=True
        )
    except subprocess.TimeoutExpired:
        return jsonify(url=escape(url), status='timeout'), 504

    status = 'alive' if result.returncode == 0 else 'unreachable'
    return jsonify(url=escape(url), status=status), 200

# ...existing code...