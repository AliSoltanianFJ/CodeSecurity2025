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

    # Connect to the database
    db = MySQLdb.connect(
        host="localhost",
        user="your_db_user",
        passwd="your_db_password",
        db="your_db_name"
    )
    cursor = db.cursor()

    # Use a parameterized query for safety
    query = "SELECT * FROM orders WHERE customer_id = %s AND status = %s"
    cursor.execute(query, (customer_id, status))
    results = cursor.fetchall()

    # Display results as plain text for demonstration
    orders_list = []
    for row in results:
        orders_list.append(str(row))

    cursor.close()
    db.close()

    return "<br>".join(orders_list)