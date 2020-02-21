FROM alpine

COPY inspectit-ocelot-agent.jar /
COPY entrypoint.sh  /
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["sh", "/entrypoint.sh"]
