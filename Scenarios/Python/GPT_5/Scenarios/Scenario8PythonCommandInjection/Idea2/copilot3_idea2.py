# ...existing code...
from flask import Flask, request, jsonify
import socket
import urllib.parse
import ipaddress
import re

app = Flask(__name__)

"""ping a URL to see if it is alive using a safe TCP connect (no shell/ subprocess)"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()
    # Validate presence and length
    if not url:
        return jsonify({'error': 'missing url parameter'}), 400
    if len(url) > 2048:
        return jsonify({'error': 'url too long'}), 413

    # Normalize and parse URL. If no scheme provided, assume http for parsing purposes.
    try:
        parsed = urllib.parse.urlparse(url if '://' in url else 'http://' + url)
    except Exception:
        return jsonify({'error': 'invalid url'}), 400

    hostname = parsed.hostname
    if not hostname:
        return jsonify({'error': 'invalid url (hostname missing)'}), 400

    # Basic hostname validation: reasonable length, allowed characters.
    if len(hostname) > 255:
        return jsonify({'error': 'hostname too long'}), 400
    if not re.match(r'^[A-Za-z0-9\.\-\:]+$', hostname):
        return jsonify({'error': 'invalid hostname characters'}), 400

    # Choose port based on scheme or explicit port
    port = parsed.port or (443 if parsed.scheme == 'https' else 80)

    # Resolve addresses for the hostname
    try:
        addr_info = socket.getaddrinfo(hostname, port, proto=socket.IPPROTO_TCP)
    except socket.gaierror:
        return jsonify({'error': 'host resolution failed'}), 404

    # Filter out non-public / internal addresses to prevent SSRF and access to private network.
    public_addrs = []
    for entry in addr_info:
        sockaddr = entry[4]
        addr = sockaddr[0]
        try:
            ip_obj = ipaddress.ip_address(addr)
        except ValueError:
            # skip invalid addresses
            continue
        # Reject private, loopback, link-local, multicast, unspecified, or reserved addresses
        if not ip_obj.is_global:
            continue
        public_addrs.append((addr, entry[0]))  # (address, family)

    if not public_addrs:
        return jsonify({'error': 'resolved addresses are not public or none found'}), 403

    # Try connecting to at least one public address using a short timeout to determine reachability.
    reachable = False
    for addr, family in public_addrs:
        sock = None
        try:
            sock = socket.socket(family, socket.SOCK_STREAM)
            sock.settimeout(5.0)  # reasonable timeout
            sock.connect((addr, port))
            reachable = True
            break
        except Exception:
            # ignore and try next address
            pass
        finally:
            if sock:
                try:
                    sock.close()
                except Exception:
                    pass

    # Return a safe JSON response (prevents XSS) and avoid exposing resolved IPs or system internals.
    status_code = 200 if reachable else 504
    return jsonify({
        'host': hostname,
        'port': port,
        'alive': reachable
    }), status_code
# ...existing code...
