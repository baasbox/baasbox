FROM dockerfile/java:oracle-java8
MAINTAINER Cesare Rocchi <c.rocchi@baasbox.com>
WORKDIR /baasbox

RUN wget --content-disposition http://www.baasbox.com/download/baasbox-stable.zip
RUN unzip baasbox*.zip
RUN rm baasbox*.zip
RUN chmod +x baasbox-0.9.2/start # weak spot when version number changes
EXPOSE 9000
ENTRYPOINT baasbox-0.9.2/start