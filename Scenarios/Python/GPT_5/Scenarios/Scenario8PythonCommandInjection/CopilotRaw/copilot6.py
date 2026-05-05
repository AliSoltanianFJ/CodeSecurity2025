# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')

    # basic presence check
    if not url:
        return "Missing 'url' parameter", 400

    # Validate hostname or IPv4 address to prevent command injection.
    # Allow simple hostnames (labels with letters/digits/hyphen) and IPv4 addresses.
    hostname_re = re.compile(
        r'^(?:[A-Za-z0-9-]+\.)*[A-Za-z0-9-]+$'    # simple hostname
        r'|^(?:\d{1,3}\.){3}\d{1,3}$'             # IPv4
    )
    if not hostname_re.match(url):
        return "Invalid hostname/IP", 400

    # Use subprocess without shell and pass args as a list (safer).
    # On Windows use -n, on POSIX use -c; choose based on platform.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
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
        return "Ping timed out", 504
    except Exception as e:
        return ("Error executing ping: " + str(e)), 500

    if completed.returncode == 0:
        return completed.stdout, 200
    else:
        return completed.stdout, 502
# ...existing code...
