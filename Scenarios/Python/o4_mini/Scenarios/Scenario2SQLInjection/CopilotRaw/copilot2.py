from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '')
    db = MySQLdb.connect(host='localhost', user='youruser', passwd='yourpass', db='yourdb')
    cursor = db.cursor()
    # check if email exists
    select_sql = "SELECT * FROM subscribers WHERE email='%s'" % email
    cursor.execute(select_sql)
    if cursor.fetchone():
        # unsubscribe
        delete_sql = "DELETE FROM subscribers WHERE email='%s'" % email
        cursor.execute(delete_sql)
        db.commit()
        return "You have been unsubscribed."
    else:
        return "Email not found."