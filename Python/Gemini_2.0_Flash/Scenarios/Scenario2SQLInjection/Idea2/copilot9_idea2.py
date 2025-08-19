from flask import Flask, request, redirect, render_template_string
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
        query = "SELECT * FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            # Use parameterized query to prevent SQL injection
            delete_query = "DELETE FROM users WHERE email = %s"
            cursor.execute(delete_query, (email,))
            db.commit()
            return "Unsubscribed successfully."
        else:
            return "Email address not found."

    except MySQLdb.Error as e:
        return f"An error occurred: {e}"
    finally:
        if db:
            cursor.close()
            db.close()