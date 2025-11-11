# ...existing code...
from flask import Flask, request, jsonify
import os
import subprocess
import sys
import re
import ipaddress
import urllib.parse

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '')
    url = (url or '').strip()
    if not url:
        return jsonify({'error': 'Missing url parameter'}), 400

    # Parse input to extract hostname only; reject any URL with credentials or ports embedded
    try:
        parsed = urllib.parse.urlparse(url if '://' in url else '//' + url, scheme='')
        host = parsed.hostname if parsed.hostname else url
    except Exception:
        return jsonify({'error': 'Invalid url parameter'}), 400

    if not isinstance(host, str) or not host:
        return jsonify({'error': 'Invalid host'}), 400

    host = host.strip()

    # Basic length checks to avoid overly long inputs (prevents buffer-like issues)
    if len(host) > 253:
        return jsonify({'error': 'Host too long'}), 400

    # Validate host: allow IPv4, IPv6, 'localhost' or well-formed domain names only
    def is_valid_host(h: str) -> bool:
        if h.lower() == 'localhost':
            return True
        # Check IP address
        try:
            ipaddress.ip_address(h)
            return True
        except ValueError:
            pass
        # Normalize trailing dot
        if h.endswith('.'):
            h = h[:-1]
        # Domain name rules: labels 1-63 chars, overall <=253, allowed chars letters/digits/hyphen
        # Require at least one dot and a TLD (prevents single-word injections)
        domain_regex = re.compile(
            r'^(?=.{1,253}$)([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)(?:\.([A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?))+\.[A-Za-z]{2,63}$',
            re.IGNORECASE
        )
        return bool(domain_regex.match(h))

    if not is_valid_host(host):
        return jsonify({'error': 'Invalid host format'}), 400

    # Build ping command without shell to avoid command injection.
    # Use platform-appropriate count flag.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', host]

    try:
        # Run ping safely without a shell, with a timeout and limited output capture.
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=5,
            shell=False
        )
        raw_output = completed.stdout or ''
        # Keep only printable characters and limit length to avoid huge responses.
        safe_output = ''.join(ch for ch in raw_output if ch.isprintable())[:2000]
        alive = completed.returncode == 0
        return jsonify({'host': host, 'alive': alive, 'output': safe_output}), (200 if alive else 200)
    except subprocess.TimeoutExpired:
        return jsonify({'host': host, 'alive': False, 'error': 'Ping timed out'}), 504
    except FileNotFoundError:
        # ping command not available on this server
        return jsonify({'error': 'Ping command not available on server'}), 503
    except Exception:
        # Generic error without exposing internal details
        return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    # Do not run with debug=True in production to avoid leaking private information
    app.run(host='127.0.0.1', port=5000, debug=False)
# ...existing code...
