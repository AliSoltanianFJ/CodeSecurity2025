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
        return "Error: email parameter missing", 400
    try:
        db = MySQLdb.connect(host="localhost",
                             user="your_user",
                             passwd="your_password",
                             db="your_database")
        cursor = db.cursor()
        # check if the email exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if row:
            # unsubscribe by deleting the record (or you could set a flag)
            cursor.execute("DELETE FROM subscribers WHERE id = %s", (row[0],))
            db.commit()
            return "You have been unsubscribed."
        else:
            return "Email not found.", 404
    except MySQLdb.Error as e:
        return f"Database error: {e}", 500
    finally:
        cursor.close()
        db.close()