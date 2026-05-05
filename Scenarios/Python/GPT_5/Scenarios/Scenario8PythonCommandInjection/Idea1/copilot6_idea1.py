from flask import Flask, request, redirect, Response
import os
import subprocess
import sys
import re
import ipaddress

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = (request.args.get('url') or '').strip()

    # Basic presence and length checks
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')
    if len(url) > 255:
        return Response("Invalid 'url' parameter", status=400, mimetype='text/plain')

    # Validate as IP (IPv4/IPv6) or a hostname. This avoids shell/meta characters.
    is_ip = False
    try:
        ipaddress.ip_address(url)
        is_ip = True
    except Exception:
        is_ip = False

    if not is_ip:
        # Hostname validation:
        # - total length 1..255
        # - labels 1..63, letters/digits, may contain hyphens but not start/end with hyphen
        hostname_re = re.compile(
            r'^(?=.{1,255}$)'
            r'[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?'
            r'(?:\.[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)*$'
        )
        if not hostname_re.fullmatch(url):
            return Response("Invalid hostname or IP address", status=400, mimetype='text/plain')

    # Platform-specific ping count flag
    count_flag = '-n' if sys.platform.startswith('win') else '-c'

    # Use argument list and do NOT use shell to prevent command injection
    cmd = ['ping', count_flag, '1', url]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            shell=False,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return Response("Ping command timed out", status=504, mimetype='text/plain')
    except Exception:
        return Response("Error executing ping", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or "", status=status, mimetype='text/plain')


if __name__ == '__main__':
    app.run(debug=True)
