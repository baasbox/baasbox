FROM dockerfile/java:oracle-java8
MAINTAINER Cesare Rocchi <c.rocchi@baasbox.com>
WORKDIR /baasbox

RUN wget --content-disposition http://www.baasbox.com/download/baasbox-stable.zip
RUN unzip baasbox*.zip
RUN rm baasbox*.zip
RUN mv baasbox-*/ baasbox/
RUN chmod +x baasbox/start
EXPOSE 9000
ENTRYPOINT baasbox/start
