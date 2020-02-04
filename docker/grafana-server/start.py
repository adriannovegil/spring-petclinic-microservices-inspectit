#!/usr/bin/env python
# Based on https://github.com/QualiApps/grafana-docker/blob/master/files/start.py
import requests
from os import environ
from time import sleep
from urlparse import urlunparse
from subprocess import Popen, PIPE

class Grafana(object):
    env = environ.get
    scheme = "http"
    api_path_datasources = "api/datasources"
    api_path_gnet = "api/gnet/dashboards/1691"
    api_path_import = "api/dashboards/import"

    def __init__(self):
        '''
            Init params
        '''
        self.params = {
            "name": self.env("DS_NAME", "inspectit-influx"),
            "type": self.env("DS_TYPE", "influxdb"),
            "access": self.env("DS_ACCESS", "proxy"),
            "url": self.env("DS_URL, "http://influxdb-server:8086"),
            "password": self.env("DS_PASS", "root"),
            "user": self.env("DS_USER", "root"),
            "database": self.env("DS_DB", "inspectit"),
            "basicAuth": self.env("DS_AUTH", 'false'),
            "basicAuthUser": self.env("DS_AUTH_USER", ""),
            "basicAuthPassword": self.env("AUTH_PASS", ""),
            "isDefault": self.env("DS_IS_DEFAULT", 'false'),
            "jsonData": self.env("DS_JSON_DATA", 'null')
        }

        # Create grafana api paths
        self.gf_url_datasources = urlunparse(
            (
                self.scheme,
                ":".join((self.env("GF_HOST", "grafana-server"), self.env("GF_PORT", "3000"))),
                self.api_path_datasources, "", "", ""
            )
        )
        self.gf_url_gnet = urlunparse(
            (
                self.scheme,
                ":".join((self.env("GF_HOST", "grafana-server"), self.env("GF_PORT", "3000"))),
                self.api_path_gnet, "", "", ""
            )
        )
        self.gf_url_import = urlunparse(
            (
                self.scheme,
                ":".join((self.env("GF_HOST", "grafana-server"), self.env("GF_PORT", "3000"))),
                self.api_path_import, "", "", ""
            )
        )
        # Init requests session
        self.auth = self.env("GF_USER", "admin"), self.env("GF_PASS", "admin")
        self.sess = requests.Session()

    def init_datasource(self):
        '''
            Upload a datasource
            :return bool
        '''
        response = False
        res = self.sess.post(self.gf_url_datasources, data=self.params, auth=self.auth)
        if res.status_code == requests.codes.ok:
            response = True

        return response

    def import_dashboard(self):
        '''
            Import the official inspectIT dashboard
        '''
        res = self.sess.get(self.gf_url_gnet, auth=self.auth)
        print res.status_code

        dashboard = res.json()

        dashboard["dashboard"] = dashboard.pop("json")
        dashboard["overwrite"] = True

        dashboard_datasource = {
            "name": "DS_INFLUXDB",
            "type": "datasource",
            "pluginId": self.params["type"],
            "value": self.params["name"]
        }
        dashboard["inputs"] = [dashboard_datasource]

        res = self.sess.post(self.gf_url_import, json=dashboard, auth=self.auth)

        print res.status_code

    def create_influx_database(self):
        '''
            Workaround for INSPECTIT-2493
        '''
        res = requests.post("http://influxdb-server:8086/query", data={'q':'CREATE DATABASE "inspectit"'})

        print res.status_code
        print res.text

    def start(self):
        '''
            Start grafana and check api
            :return tuple - status, grafana process
        '''
        status = False
        # run grafana
        gf_proc = Popen(["/run.sh"], stdout=PIPE)

        # wait, until gf api will be available
        # trying 5 times
        retry = 0
        while retry <= 5:
            if self._check_gf():
                status = True
                break
            retry += 1
            sleep(3)

        return status, gf_proc

    def _check_gf(self):
        '''
            Check gf api
            :return bool
        '''
        resp = False
        try:
            res = self.sess.get(self.gf_url_datasources, auth=self.auth)
            resp = True if res and res.status_code == requests.codes.ok else False
        except Exception as message:
            print "CONNECTION! %s" % message

        return resp

if __name__ == "__main__":
    gf = Grafana()
    try:
        exit_code = 0
        status, gf_proc = gf.start()
        if status:
            if gf.init_datasource():
                print "*------------SUCCESS! Your datasource was added!------------*"
                gf.create_influx_database()
                print "*------------SUCCESS! Influx database created!--------------*"
                gf.import_dashboard()
                print "*------------SUCCESS! Dashboard was added!------------------*"
                with gf_proc.stdout:
		            for line in iter(gf_proc.stdout.readline, b''):
			            print line,
		            gf_proc.wait() # wait for the subprocess to exit

            exit_code = gf_proc.poll()
    except Exception as error:
        print "*------------ERROR! %s------------*" % error
        exit_code = 1
    finally:
        exit(exit_code)
