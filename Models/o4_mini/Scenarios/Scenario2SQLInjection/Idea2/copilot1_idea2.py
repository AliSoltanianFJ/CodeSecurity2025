from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import re
from flask import escape, abort

EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        abort(400, description="Email parameter is required")
    if not EMAIL_REGEX.match(email):
        abort(400, description="Invalid email address")

    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        # parameterized query prevents SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        if cursor.fetchone():
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            conn.commit()
            return escape("You have been unsubscribed successfully.")
        else:
            abort(404, description="Email not found")
    except MySQLdb.Error:
        abort(500, description="Internal server error")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass