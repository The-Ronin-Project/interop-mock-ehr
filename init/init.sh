#!/bin/bash
for a in /init/*; do
  for b in $a/*; do
    for c in $b/*.json; do
      mysqlsh -h mockehrmysql -P 33060 -u springuser -pThePassword -D mock_ehr_db --import $c ${b##*/}
    done
  done
done
exit 0
