#!/bin/bash
for f in /init/*.json; do
  mysqlsh -h mockehrmysql -P 33060 -u springuser -pThePassword -D mock_ehr_db --import $f
done
exit
