
from flask import Flask, request, abort, jsonify
import subprocess
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '')
    # 1) Validate: only letters, digits, hyphens, dots; length <=255
    if not re.fullmatch(r'[A-Za-z0-9\.-]{1,255}', url):
        abort(400, 'Invalid URL parameter')
    try:
        # 2) No shell, list args only
        result = subprocess.run(
            ['ping', '-c', '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        abort(504, 'Ping timed out')
    return jsonify({
        'returncode': result.returncode,
        'stdout': result.stdout,
        'stderr': result.stderr
    })