#!/bin/bash
mysql -h mockehrmysql -P 3306 -u springuser -pThePassword <<MY_QUERY
DROP DATABASE IF EXISTS mock_ehr_db;
CREATE DATABASE mock_ehr_db;
MY_QUERY
for a in /init/*; do
  for b in $a/*; do
    for c in $b/*.json; do
      mysqlsh -h mockehrmysql -P 33060 -u springuser -pThePassword -D mock_ehr_db --import $c ${b##*/}
    done
  done
done
exit 0
