from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re, socket, ipaddress, subprocess

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    # 1) basic hostname validation
    if not re.fullmatch(r'[A-Za-z0-9\-\.]+', url):
        return "Invalid host", 400

    # 2) resolve and block non-public IPs
    try:
        addr = socket.gethostbyname(url)
        ip = ipaddress.ip_address(addr)
        if ip.is_private or ip.is_loopback or ip.is_reserved:
            return "Host not allowed", 403
    except socket.gaierror:
        return "Host not found", 404

    # 3) perform ping safely
    try:
        proc = subprocess.run(
            ['ping', '-c', '3', '-W', '2', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504

    if proc.returncode == 0:
        return proc.stdout, 200
    else:
        return proc.stderr, 502