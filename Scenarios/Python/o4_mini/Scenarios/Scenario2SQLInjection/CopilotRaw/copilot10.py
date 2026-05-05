from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # get email parameter
    email = request.args.get('email')
    if not email:
        return 'Missing email parameter', 400

    try:
        # connect to MySQL (adjust env vars or hard‐code as needed)
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mydb')
        )
        cursor = conn.cursor()

        # check if email exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        if not cursor.fetchone():
            return 'Email not found', 404

        # unsubscribe (e.g. mark unsubscribed or delete)
        cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
        conn.commit()
    except MySQLdb.Error as e:
        return f'Database error: {e}', 500
    finally:
        cursor.close()
        conn.close()

    return 'You have been unsubscribed successfully', 200