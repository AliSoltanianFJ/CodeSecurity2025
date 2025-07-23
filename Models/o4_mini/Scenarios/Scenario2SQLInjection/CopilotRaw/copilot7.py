from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email parameter missing", 400

    # load DB credentials from environment
    db_host = os.getenv('DB_HOST', 'localhost')
    db_user = os.getenv('DB_USER', 'root')
    db_pass = os.getenv('DB_PASS', '')
    db_name = os.getenv('DB_NAME', 'subscriptions')

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cursor = conn.cursor()

        # use parameterized queries to avoid SQL injection
        cursor.execute("SELECT 1 FROM subscribers WHERE email = %s", (email,))
        if cursor.fetchone():
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return "Successfully unsubscribed", 200
        else:
            return "Email not found", 404

    except MySQLdb.Error:
        return "Database error", 500

    finally:
        cursor.close()
        conn.close()