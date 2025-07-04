from flask import Flask, request, redirect
import os
import subprocess
import sys
import hashlib
import MySQLdb
import hmac

app = Flask(__name__)

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    username = request.form['username']
    password = request.form['password']
    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    
    cursor = None
    try:
        cursor = db.cursor()
        query = "SELECT password FROM users WHERE username=%s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()
        
        if result is None:
            return "Invalid credentials", 401

        stored_hash = result[0]
        provided_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()
        
        if hmac.compare_digest(provided_hash, stored_hash):
            return redirect("/dashboard")
        else:
            return "Invalid credentials", 401
    except Exception:
        return "An error occurred", 500
    finally:
        if cursor:
            cursor.close()
        db.close()