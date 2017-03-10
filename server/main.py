from BaseHTTPServer import HTTPServer
from BaseHTTPServer import BaseHTTPRequestHandler
import Cookie
import zlib

# HTTPRequestHandler class
class testHTTPServer_RequestHandler(BaseHTTPRequestHandler):


 
  # GET
  def do_GET(self):
    # Send response status code
    self.send_response(200)
 
    # Send headers
    self.send_header('Content-type','text/html')
    self.end_headers()
 
    # Send message back to client
    message = "Hello world!"
    # Write content as utf-8 data
    self.wfile.write(bytes(message, "utf8"))
    return

  def do_POST(self):
    gz_blob = self.rfile.read(int(self.headers['Content-Length']))
    print(self.path)
    c = Cookie.SimpleCookie()
    c.load(self.headers.get("Cookie"))
    print(c['user'].value)
    self.send_response(200)
    self.send_header('Content-type','text/html')
    self.end_headers()

    message = zlib.decompress(gz_blob, 16+zlib.MAX_WBITS)


    print(message)

    return
 
def run():
  print('starting server...')
 
  # Server settings
  # Choose port 8080, for port 80, which is normally used for a http server, you need root access
  server_address = ('0.0.0.0', 8081)
  httpd = HTTPServer(server_address, testHTTPServer_RequestHandler)
  print('running server...')
  httpd.serve_forever()
 
 
run()
