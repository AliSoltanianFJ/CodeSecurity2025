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
        return 'No email provided', 400

    try:
        db = MySQLdb.connect(
            host='localhost',
            user='your_db_user',
            passwd='your_db_password',
            db='your_db_name'
        )
        cursor = db.cursor(MySQLdb.cursors.DictCursor)

        # Safely query for the subscriber
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        subscriber = cursor.fetchone()

        if not subscriber:
            return 'Email not found', 404

        # Unsubscribe by deleting the record
        cursor.execute("DELETE FROM subscribers WHERE id = %s", (subscriber['id'],))
        db.commit()
        return 'Unsubscribed successfully', 200

    except MySQLdb.Error as e:
        if db:
            db.rollback()
        return f'Database error: {e}', 500

    finally:
        if cursor:
            cursor.close()
        if db:
            db.close()