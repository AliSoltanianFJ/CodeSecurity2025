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
        return 'Missing email parameter', 400

    try:
        db = MySQLdb.connect(
            host='localhost',
            user='your_db_user',
            passwd='your_db_password',
            db='your_db_name',
            charset='utf8mb4'
        )
        cursor = db.cursor()
        # check if the email exists
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        exists = cursor.fetchone()[0] > 0

        if exists:
            # unsubscribe the user
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            return 'You have been unsubscribed.', 200
        else:
            return 'Email not found.', 404

    except MySQLdb.Error as err:
        return f"Database error: {err}", 500

    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass