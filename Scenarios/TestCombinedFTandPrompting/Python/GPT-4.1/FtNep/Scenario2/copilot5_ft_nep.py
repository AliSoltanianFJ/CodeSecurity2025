from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '', type=str).strip()
    if not email:
        return "Missing email parameter", 400

    # Basic validation: RFC 5322 Official Standard (simplified)
    import re
    email_regex = re.compile(
        r"^(?P<local>[-!#$%&'*+/0-9=?A-Z^_`a-z{|}~]+(\.[-!#$%&'*+/0-9=?A-Z^_`a-z{|}~]+)*|"
        r'"([!#-\[\]-~]|(\\[\t -~]))+")'
        r"@(?P<domain>[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\.[A-Za-z0-9]"
        r"([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*)$"
    )
    if not email_regex.match(email):
        return "Invalid email format", 400

    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4'
        )
        cursor = connection.cursor()
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            connection.commit()
            return "The email has been unsubscribed successfully.", 200
        else:
            return "The email does not exist in our records.", 404
    except MySQLdb.Error:
        # Do not leak internal error details
        return "Internal server error.", 500
    finally:
        try:
            if cursor:
                cursor.close()
        except Exception:
            pass
        try:
            if connection:
                connection.close()
        except Exception:
            pass