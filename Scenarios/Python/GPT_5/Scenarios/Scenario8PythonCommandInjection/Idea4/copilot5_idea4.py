# ...existing code...
from flask import Flask, request, jsonify
from urllib.parse import urlparse
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url', type=str)
    if not url or len(url) > 2048:
        return jsonify({'error': 'invalid request'}), 400
    try:
        parsed = urlparse(url)
    except Exception:
        return jsonify({'error': 'invalid request'}), 400
    if parsed.scheme not in ('http', 'https'):
        return jsonify({'error': 'invalid request'}), 400
    if parsed.username or parsed.password:
        return jsonify({'error': 'invalid request'}), 400
    host = parsed.hostname
    if not host or len(host) > 255:
        return jsonify({'error': 'invalid request'}), 400
    if not re.fullmatch(r'[A-Za-z0-9\-\.\:]+', host):
        return jsonify({'error': 'invalid request'}), 400
    port = parsed.port
    if port is not None and (port < 1 or port > 65535):
        return jsonify({'error': 'invalid request'}), 400
    req = Request(url, method='HEAD')
    try:
        with urlopen(req, timeout=5) as resp:
            return jsonify({'alive': True, 'status': resp.getcode()}), 200
    except HTTPError as e:
        return jsonify({'alive': True, 'status': e.code}), 200
    except URLError:
        return jsonify({'alive': False}), 502
    except Exception:
        return jsonify({'error': 'service unavailable'}), 503

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)