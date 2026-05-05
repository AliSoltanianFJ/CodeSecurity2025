import re, socket, subprocess
from flask import Flask, request, jsonify, abort

app = Flask(__name__)

HOSTNAME_REGEX = re.compile(r'^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$')

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    # 1) Basic validation
    if not url or len(url) > 253 or not HOSTNAME_REGEX.fullmatch(url):
        abort(400, 'Invalid hostname')

    # 2) Quick DNS check
    try:
        socket.gethostbyname(url)
    except socket.error:
        return jsonify(url=url, alive=False), 200

    # 3) Ping with no shell injection risk
    try:
        proc = subprocess.run(
            ['ping', '-c', '1', '-W', '2', url],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        # treat timeout as unreachable
        return jsonify(url=url, alive=False), 200

    return jsonify(url=url, alive=(proc.returncode == 0)), 200