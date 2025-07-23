from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    # Demonstration of insecure command injection vulnerability
    # Directly interpolates user input into the command string
    try:
        command = f"ping -n 1 {url}"
        output = os.popen(command).read()
        return f"<pre>{output}</pre>"
    except Exception as e:
        return f"Error: {str(e)}", 500
# ...existing code...