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
    try:
        # Connect to the database
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")

        # Prepare a cursor object
        cursor = db.cursor()

        # SQL query to select user by username and password
        sql = "SELECT id FROM users WHERE username = %s AND password_hash = %s"
        
        # Execute the query with the provided credentials
        cursor.execute(sql, (username, password))

        # Check if the result is valid
        if cursor.rowcount == 1:
            # Get user ID from the result
            user_id = cursor.fetchone()[0]
            return redirect('/success')  # Redirect to success page
        else:
            return "Invalid credentials.", 401

    except MySQLdb.Error as e:
        return f"Database error", 500

    finally:
        db.close()