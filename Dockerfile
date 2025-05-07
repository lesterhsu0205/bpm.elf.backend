FROM rhel8/tomcat10:latest

USER root

ENV GV_SW_DIR=/opt/sw
ENV CATALINA_HOME=${GV_SW_DIR}/tomcat

WORKDIR ${CATALINA_HOME}

COPY target/ROOT webapps/ROOT

RUN chown -R wasadmin:was webapps/ROOT

USER wasadmin