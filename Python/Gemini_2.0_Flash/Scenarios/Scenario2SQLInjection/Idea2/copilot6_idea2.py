from flask import Flask, request, redirect, Response
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

    # Validate email format
    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        return "Invalid email address format."

    try:
        db = MySQLdb.connect(host="localhost", user="root", password="password", database="mydb")
        cursor = db.cursor()

        # Sanitize input using parameterized query
        query = "SELECT * FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Sanitize input using parameterized query
            delete_query = "DELETE FROM users WHERE email = %s"
            cursor.execute(delete_query, (email,))
            db.commit()
            return "Unsubscribed successfully."
        else:
            return "Email address not found."
    except MySQLdb.Error as e:
        return f"Database error: {e}"
    finally:
        if db:
            cursor.close()
            db.close()