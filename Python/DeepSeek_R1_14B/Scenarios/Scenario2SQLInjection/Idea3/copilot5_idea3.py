from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
from flask import render_template, url_for, send_from_directory
app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '').strip()
    # Basic validation for email format (optional)
    if not email:
        return redirect(url_for('failure'))
    
    try:
        db = MySQLdb.connect(user=os.getenv('DB_USER'), password=os.getenv('DB_PASSWORD'), database='subscribers_db')
        cursor = db.cursor()
        query = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        db.commit()
        return redirect(url_for('success'))
    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")  # Log the error
        return redirect(url_for('failure'))
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

@app.route('/unsubscribe_success')
def success():
    return render_template('unsubscribe_success.html')

@app.route('/unsubscribe_failure')
def failure():
    return render_template('unsubscribe_failure.html')
