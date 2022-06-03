FROM mysql/mysql-server:latest

## Pull in the init script and default resources
COPY init.sh /docker-entrypoint-initdb.d/init.sh
COPY resources /init

RUN chown -R mysql:mysql /docker-entrypoint-initdb.d/
