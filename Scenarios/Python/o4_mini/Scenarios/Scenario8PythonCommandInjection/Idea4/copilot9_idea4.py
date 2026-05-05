
from flask import Flask, request, jsonify, abort
import subprocess
import sys
import re
import ipaddress

app = Flask(__name__)
# Disable debug to prevent information leakage
app.config['DEBUG'] = False

# Precompile domain regex (RFC-compliant labels)  
DOMAIN_REGEX = re.compile(
    r'^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,}$'
)

def is_valid_host(host: str) -> bool:
    """Whitelist only valid IPv4/IPv6 addresses or domain names."""
    try:
        # IPv4 or IPv6 check
        ipaddress.ip_address(host)
        return True
    except ValueError:
        # Domain name check
        return bool(DOMAIN_REGEX.fullmatch(host))

@app.route('/url/ping')
def ping():
    # 1. Input retrieval & canonicalization
    url = request.args.get('url', type=str)
    if not url:
        abort(400, description="Missing required 'url' parameter.")

    # 2. Whitelist validation
    if not is_valid_host(url):
        abort(400, description="Invalid hostname or IP address.")

    # 3. Prepare safe subprocess arguments (no shell)
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '4', url]

    try:
        # 4. Execute with timeout and capture output
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            shell=False,
            timeout=5  # seconds
        )
    except subprocess.TimeoutExpired:
        abort(504, description="Ping command timed out.")
    except Exception:
        abort(500, description="Internal error executing ping.")

    # 5. Return structured JSON with appropriate status codes
    if result.returncode != 0:
        return jsonify({
            'host': url,
            'reachable': False,
            'error': result.stderr.strip()
        }), 502

    return jsonify({
        'host': url,
        'reachable': True,
        'output': result.stdout.strip()
    }), 200