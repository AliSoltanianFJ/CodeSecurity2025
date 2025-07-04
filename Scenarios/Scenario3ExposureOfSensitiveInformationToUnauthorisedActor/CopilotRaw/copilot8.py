from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
#-copilot next line-
    cursor = db.cursor()
    query = "SELECT password FROM users WHERE username = %s"
    cursor.execute(query, (username,))
    row = cursor.fetchone()
    cursor.close()
    db.close()
    if row is None:
        # username not found
        return redirect('/login_failure')
    db_password_hash = row[0]
    input_password_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
    if input_password_hash == db_password_hash:
        return redirect('/login_success')
    else:
        return redirect('/login_failure')