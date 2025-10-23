#!/usr/bin/env python3

from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse
import sys
import threading
import binascii
import json

apiOrg = 'sentry-sdks'
apiProject = 'sentry-android'
uri = urlparse(sys.argv[1] if len(sys.argv) > 1 else 'http://127.0.0.1:8000')
version='1.1.0'
appIdentifier='com.sentry.fastlane.app'

class Handler(BaseHTTPRequestHandler):
    body = None

    def do_GET(self):
        self.start_response(HTTPStatus.OK)

        if self.path == "/STOP":
            print("HTTP server stopping!")
            threading.Thread(target=self.server.shutdown).start()
            return

        if self.isApi('api/0/organizations/{}/chunk-upload/'.format(apiOrg)):
            self.writeJSON('{"url":"' + uri.geturl() + self.path + '",'
                           '"chunkSize":8388608,"chunksPerRequest":64,"maxFileSize":2147483648,'
                           '"maxRequestSize":33554432,"concurrency":1,"hashAlgorithm":"sha1","compression":["gzip"],'
                           '"accept":["debug_files","release_files","pdbs","sources","bcsymbolmaps","preprod_artifacts"]}')
        elif self.isApi('/api/0/organizations/{}/repos/?cursor='.format(apiOrg)):
            self.writeJSONFile("test/assets/repos.json")
        elif self.isApi('/api/0/organizations/{}/releases/{}/previous-with-commits/'.format(apiOrg, version)):
            self.writeJSONFile("test/assets/release.json")
        elif self.isApi('/api/0/projects/{}/{}/releases/{}/files/?cursor='.format(apiOrg, apiProject, version)):
            self.writeJSONFile("test/assets/artifacts.json")
        else:
            self.end_headers()

        self.flushLogs()

    def do_POST(self):
        self.start_response(HTTPStatus.OK)

        if self.isApi('api/0/projects/{}/{}/files/difs/assemble/'.format(apiOrg, apiProject)):
            # Request body example:
            # {
            #   "9a01653a...":{"name":"UnityPlayer.dylib","debug_id":"eb4a7644-...","chunks":["f84d3907945cdf41b33da8245747f4d05e6ffcb4", ...]},
            #   "4185e454...":{"name":"UnityPlayer.dylib","debug_id":"86d95b40-...","chunks":[...]}
            # }
            # Response body to let the CLI know we have the symbols already (we don't need to test the actual upload):
            # {
            #   "9a01653a...":{"state":"ok","missingChunks":[]},
            #   "4185e454...":{"state":"ok","missingChunks":[]}
            # }
            jsonRequest = json.loads(self.body)
            jsonResponse = '{'
            for key, value in jsonRequest.items():
                jsonResponse += '"{}":{{"state":"ok","missingChunks":[],"uploaded_id":"{}"}},'.format(
                    key, value['debug_id'])
                self.log_message('Received: %40s %40s %s', key,
                                 value['debug_id'], value['name'])
            jsonResponse = jsonResponse.rstrip(',') + '}'
            self.writeJSON(jsonResponse)
        elif self.isApi('api/0/projects/{}/{}/releases/'.format(apiOrg, apiProject)):
            self.writeJSONFile("test/assets/release.json")
        elif self.isApi('/api/0/organizations/{}/releases/{}@{}/deploys/'.format(apiOrg, appIdentifier, version)):
            self.writeJSONFile("test/assets/deploy.json")
        elif self.isApi('/api/0/projects/{}/{}/releases/{}@{}/files/'.format(apiOrg, apiProject, appIdentifier, version)):
            self.writeJSONFile("test/assets/artifact.json")
        elif self.isApi('/api/0/organizations/{}/releases/{}/assemble/'.format(apiOrg, version)):
            self.writeJSONFile("test/assets/assemble-artifacts-response.json")
        elif self.isApi('/api/0/projects/{}/{}/files/preprodartifacts/assemble/'.format(apiOrg, apiProject)):
            # Handle preprod artifacts assemble request
            # Expected request: {"checksum":"...", "chunks":["..."]}
            # Expected response: AssembleBuildResponse struct
            jsonRequest = json.loads(self.body)
            checksum = jsonRequest.get('checksum', '')
            artifactUrl = '{}/artifacts/{}'.format(uri.geturl(), checksum)
            jsonResponse = '{{"state":"ok","missingChunks":[],"artifactUrl":"{}"}}'.format(artifactUrl)
            self.writeJSON(jsonResponse)
        elif self.isApi('api/0/organizations/{}/chunk-upload/'.format(apiOrg)):
            # Handle chunk upload POST requests
            self.writeJSON('{"state":"ok"}')
        elif self.isApi('/api/0/projects/{}/{}/files/dsyms/'.format(apiOrg, apiProject)):
            self.writeJSONFile("test/assets/debug-info-files.json")
        elif self.isApi('/api/0/projects/{}/{}/files/dsyms/associate/'.format(apiOrg, apiProject)):
            self.writeJSONFile("test/assets/associate-dsyms-response.json")
        else:
            self.end_headers()

        self.flushLogs()

    def do_PUT(self):
        self.start_response(HTTPStatus.OK)

        if self.isApi('/api/0/organizations/{}/releases/{}/'.format(apiOrg, version)):
            self.writeJSONFile("test/assets/release.json")
        else:
            self.end_headers()

        self.flushLogs()

    def start_response(self, code):
        self.body = None
        self.log_request(code)
        self.send_response_only(code)

    def log_request(self, code=None, size=None):
        if isinstance(code, HTTPStatus):
            code = code.value
        body = self.body = self.requestBody()
        if body:
            body = self.body[0:min(1000, len(body))]
        self.log_message('"%s" %s %s%s',
                         self.requestline, str(code), "({} bytes)".format(size) if size else '', body)

    # Note: this may only be called once during a single request - can't `.read()` the same stream again.
    def requestBody(self):
        if self.command == "POST" and 'Content-Length' in self.headers:
            length = int(self.headers['Content-Length'])
            content = self.rfile.read(length)
            try:
                return content.decode("utf-8")
            except:
                return binascii.hexlify(bytearray(content))
        return None

    def isApi(self, api: str):
        if self.path.strip('/') == api.strip('/'):
            self.log_message("Matched API endpoint {}".format(api))
            return True
        return False

    def writeJSONFile(self, file_name: str):
        json_file = open(file_name, "r")
        self.writeJSON(json_file.read())
        json_file.close()

    def writeJSON(self, string: str):
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(str.encode(string))

    def flushLogs(self):
        sys.stdout.flush()
        sys.stderr.flush()


print("HTTP server listening on {}".format(uri.geturl()))
print("To stop the server, execute a GET request to {}/STOP".format(uri.geturl()))
httpd = ThreadingHTTPServer((uri.hostname, uri.port), Handler)
target = httpd.serve_forever()
