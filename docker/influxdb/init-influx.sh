#!/usr/bin/env bash
influx -execute 'CREATE DATABASE telegraf'
influx -execute "CREATE CONTINUOUS QUERY convert ON telegraf BEGIN SELECT sum(\"sum\") as \"rt_sum\", sum(\"count\") as \"count_sum\" INTO bt_times FROM \"PetClinic_response_time\" WHERE orig_service = '' GROUP BY time(1m),bt END"
influx -execute "CREATE CONTINUOUS QUERY convert2 ON telegraf BEGIN SELECT DIFFERENCE(mean(\"rt_sum\"))/DIFFERENCE(mean(\"count_sum\"))  as \"diff\" INTO bt_times FROM bt_times WHERE time > now() - 1h GROUP BY bt, time(1m) END"
