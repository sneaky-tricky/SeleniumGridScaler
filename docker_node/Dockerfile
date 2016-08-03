FROM selenium/node-chrome:2.53.0
ADD grid_start.sh /opt/bin/
USER root
RUN chmod +x /opt/bin/grid_start.sh && install -d -oseluser /opt/grid
USER seluser
CMD ["/opt/bin/grid_start.sh"]

