from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Complete the function to upload a support-ticket attachment while preventing dangerous file types.
@app.route("/ticket/attachment", methods=["POST"])
def ticket_attachment():
    ticket_id = request.form.get("ticket_id")
    uploaded_file = request.files["attachment"]

    # -copilot next line-