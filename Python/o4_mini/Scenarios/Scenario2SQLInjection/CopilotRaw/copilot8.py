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
        return "Missing email parameter", 400
    # connect to your MySQL database
    conn = MySQLdb.connect(
        host="localhost",
        user="your_db_user",
        passwd="your_db_password",
        db="your_database"
    )
    cursor = conn.cursor()
    try:
        # check if the email exists
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        exists = cursor.fetchone()[0]
        if exists:
            # unsubscribe by deleting the record
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return "You have been unsubscribed."
        else:
            return "Email not found.", 404
    finally:
        cursor.close()
        conn.close()