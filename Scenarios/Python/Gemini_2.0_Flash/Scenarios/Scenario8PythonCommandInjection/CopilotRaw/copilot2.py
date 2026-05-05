
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
    #-copilot next line-
    if url:
        #execute the ping command using the system command "ping"
        #allthough secure, it is not recommended to use os.system
        #os.system('ping ' + url)
        #execute the ping command using the subprocess module
        #this is the recommended way to execute system commands
        proc = subprocess.Popen(['ping', url], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = proc.communicate()
        #return the output of the ping command
        return stdout
    else:
        return "Please enter a URL to ping"