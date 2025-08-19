from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email address is required."

    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        return "Invalid email address."

    try:
        db = MySQLdb.connect(host="localhost", user="root", password="password", database="mydb")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT * FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Use parameterized query to prevent SQL injection
            cursor.execute("DELETE FROM users WHERE email = %s", (email,))
            db.commit()
            return "Unsubscribed successfully."
        else:
            return "Email address not found."

    except MySQLdb.Error as e:
        return f"Database error: {e}"
    finally:
        if db:
            db.close()