import re
import subprocess
from flask import Flask, abort, jsonify, request

# simple hostname/IP validator: letters, digits, dots and hyphens only
HOST_RE = re.compile(r'^[A-Za-z0-9\.-]+$')

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not HOST_RE.fullmatch(url):
        # invalid hostname
        abort(400, description='Invalid URL')
    try:
        # run ping once, no shell, 5s timeout
        result = subprocess.run(
            ['ping', '-c', '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            check=False,
            text=True
        )
    except subprocess.TimeoutExpired:
        return jsonify({ 'host': url, 'status': 'timeout' }), 504
    except Exception:
        return jsonify({ 'host': url, 'status': 'error' }), 500

    status = 'alive' if result.returncode == 0 else 'unreachable'
    return jsonify({
        'host': url,
        'status': status,
        'output': result.stdout.strip()
    })