from flask import Flask, request, jsonify
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # Connect to the database (replace credentials as needed)
    db = MySQLdb.connect(user="user", passwd="password", db="mydb", host="localhost")
    cur = db.cursor(MySQLdb.cursors.DictCursor)

    # Use parameter binding to prevent SQL injection
    query = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
    cur.execute(query, (customer_id, status))
    rows = cur.fetchall()
    cur.close()
    db.close()

    return jsonify(rows)