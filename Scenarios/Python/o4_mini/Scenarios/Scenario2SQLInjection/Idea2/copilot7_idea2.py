from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import re
from flask import abort

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', default='', type=str)
    # Validate email format
    email_pattern = re.compile(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')
    if not email_pattern.match(email):
        abort(400)

    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        # Parameterized query to prevent SQL injection
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()
        if not row:
            return "No subscription found for this email.", 404

        cursor.execute(
            "DELETE FROM subscribers WHERE id = %s",
            (row[0],)
        )
        db.commit()
        return "You have been unsubscribed.", 200

    except MySQLdb.Error:
        abort(500)

    finally:
        if 'db' in locals():
            db.close()