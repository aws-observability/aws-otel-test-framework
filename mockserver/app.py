from flask import Flask
import os

app = Flask(__name__)
get_data = False

@app.route("/check-data")
def check_data():
    global get_data
    return str(get_data)

@app.route('/', defaults={'path': ''}, methods = ['POST', 'GET', 'PUT', 'DELETE'])
@app.route('/<path:path>', methods = ['POST', 'GET', 'PUT', 'DELETE'])
def catch_all(path):
    global get_data
    get_data = True
    return "{}"

if __name__ == "__main__":
    if "LISTEN_ADDRESS" not in os.environ:
        host, port = "127.0.0.1", "8080"
    else:
        address = os.environ['LISTEN_ADDRESS']
        host, port = address.split(":")
    app.run(host=host, port=int(port), debug=True)