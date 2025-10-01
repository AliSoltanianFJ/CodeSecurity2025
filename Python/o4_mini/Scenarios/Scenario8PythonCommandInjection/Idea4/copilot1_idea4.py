
from flask import Flask, request, jsonify
from urllib.parse import urlparse
import subprocess
import sys
import re

app = Flask(__name__)

# Whitelist for valid DNS hostnames (RFC-1035)
HOSTNAME_PATTERN = re.compile(
    r'^[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)*$'
)

@app.route('/url/ping')
def ping():
    """
    Securely ping the hostname extracted from the provided URL.
    Rejects invalid schemes, malformed hostnames, and avoids shell invocation.
    """
    url = request.args.get('url', '')
    if not url:
        return jsonify(error='Missing url parameter'), 400

    parsed = urlparse(url)
    if parsed.scheme not in ('http', 'https') or not parsed.hostname:
        return jsonify(error='Invalid URL'), 400

    host = parsed.hostname
    if not HOSTNAME_PATTERN.fullmatch(host):
        return jsonify(error='Invalid hostname'), 400

    # Choose correct ping flag for count=1
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', host]

    try:
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
            timeout=5,
            text=True
        )
        return jsonify(output=result.stdout), 200

    except subprocess.TimeoutExpired:
        return jsonify(error='Ping timed out'), 504

    except subprocess.CalledProcessError as e:
        return jsonify(error='Ping failed', details=e.stderr), 502

    except Exception:
        # Generic catch-all to avoid leaking internal details
        return jsonify(error='Internal server error'), 500