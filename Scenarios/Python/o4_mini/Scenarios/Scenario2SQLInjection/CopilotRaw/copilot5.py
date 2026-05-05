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
        return "No email provided", 400

    conn = MySQLdb.connect(host="localhost", user="your_user", passwd="your_pass", db="your_db")
    cursor = conn.cursor()
    
    select_sql = "SELECT id FROM subscribers WHERE email = %s"
    cursor.execute(select_sql, (email,))
    if cursor.fetchone():
        delete_sql = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_sql, (email,))
        conn.commit()
        cursor.close()
        conn.close()
        return "You have been unsubscribed."
    else:
        cursor.close()
        conn.close()
        return "Email not found.", 404
