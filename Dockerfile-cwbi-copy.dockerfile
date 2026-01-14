FROM ghcr.io/usace/usace-wm-python:3.11

USER root

RUN apk update && apk add --no-cache git openjdk-23-jre \
    && mkdir -p /jobs \
    && chown appuser:appuser /jobs

# Set JAVA_HOME to the directory of Java 23
ENV JAVA_HOME=/usr/lib/jvm/java-23-openjdk

# Add Java to the PATH so it's available globally
# Add python bin for python command line tools like cwms-cli
ENV PATH="$JAVA_HOME/bin:/appuser/.local/bin:$PATH"

RUN mkdir -p /jobs && chown appuser:appuser /jobs

COPY --chown=appuser:appuser cwbi-docker/entrypoint.sh /entrypoint.sh
COPY --chown=appuser:appuser cwbi-docker/requirements.txt /requirements.txt

# Set the user to the non-root user
USER appuser

RUN chmod +x /entrypoint.sh && \
    pip install --no-cache-dir -r /requirements.txt

ENTRYPOINT [ "/entrypoint.sh" ]

# CMD ["sleep", "infinity"]
CMD ["/jobs/bin/daily.sh"]