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
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
    cursor = db.cursor()
    
    try:
        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT password FROM users WHERE username = %s", (username,))
        result = cursor.fetchone()

        if not result:
            # Username does not exist or case-sensitive mismatch
            return redirect('/login')

        stored_hash = result[0]
        input_hash = hashlib.md5(password.encode()).hexdigest()

        if stored_hash == input_hash:
            # Successful login, redirect to home page or dashboard
            return redirect('/')
        else:
            # Incorrect password
            return redirect('/login')
    except MySQLdb.Error as e:
        app.logger.error(f"MySQL error: {e}")
        return redirect('/login')
    finally:
        cursor.close()
        db.close()