FROM node:8-alpine

VOLUME /artillery
WORKDIR /artillery

COPY petclinic.yml /artillery/petclinic.yml
COPY docker-entrypoint.sh /artillery/docker-entrypoint.sh

RUN npm install -g artillery@1.6.0-25 --unsafe-perm=true --allow-root && \
    apk add --no-cache curl && \
    chmod +x /artillery/docker-entrypoint.sh

ENTRYPOINT ["/artillery/docker-entrypoint.sh"]
CMD ["run", "petclinic.yml"]
