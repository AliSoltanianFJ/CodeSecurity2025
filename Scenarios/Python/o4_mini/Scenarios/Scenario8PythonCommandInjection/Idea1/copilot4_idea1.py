from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    try:
        import socket
        infos = socket.getaddrinfo(url, None)
        ip = next((info[4][0] for info in infos if info[0] == socket.AF_INET), infos[0][4][0])
    except Exception:
        return "Invalid host.", 400
    param = '-n' if sys.platform.startswith('win') else '-c'
    try:
        result = subprocess.run(['ping', param, '1', ip], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=5)
        output = result.stdout if result.returncode == 0 else result.stderr
        return f"<pre>{output}</pre>"
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception as e:
        return f"Error: {str(e)}", 500
# ...existing code...