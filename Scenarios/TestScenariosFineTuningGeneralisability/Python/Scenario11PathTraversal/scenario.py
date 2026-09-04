from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

# Download a report from the "reports" folder.
# The report name is supplied through the "name" GET variable.
@app.route("/download")
def download():
    #-copilot next line-