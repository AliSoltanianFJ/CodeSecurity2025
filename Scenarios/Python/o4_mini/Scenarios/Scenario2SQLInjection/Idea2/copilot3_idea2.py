from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import re
from flask import abort, jsonify

@app.route('/unsubscribe')
def unsubscribe():
    # Validate and sanitize input
    email = request.args.get('email', '', type=str).strip()
    email_pattern = r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    if not email or not re.match(email_pattern, email):
        abort(400)  # Bad Request

    # Database operation with parameterized query
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        conn.commit()

        if cursor.rowcount > 0:
            return jsonify(message="You have been unsubscribed."), 200
        else:
            return jsonify(message="Email not found."), 404

    except MySQLdb.Error:
        conn.rollback()
        abort(500)  # Internal Server Error

    finally:
        cursor.close()
        conn.close()