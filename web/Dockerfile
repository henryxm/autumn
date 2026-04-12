# Rocky Linux 9 base image mirrored to private registry
FROM docker.minclouds.com/library/rocklinux:9

# set label
LABEL maintainer="minclouds@163.com"
#设置系统编码
RUN dnf install -y glibc-langpack-zh glibc-common
RUN echo "LANG=zh_CN.UTF-8" > /etc/locale.conf
ENV LANG=zh_CN.UTF-8
ENV LC_ALL=zh_CN.UTF-8

# set environment
ENV JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk" \
    TIME_ZONE="Asia/Shanghai"
WORKDIR /$BASE_DIR

RUN set -x \
    && dnf update -y \
    && dnf install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel \
    && ln -snf /usr/share/zoneinfo/$TIME_ZONE /etc/localtime && echo '$TIME_ZONE' > /etc/timezone \
    && dnf install -y --allowerasing openssl openssl-devel curl gcc gcc-c++ zip unzip git \
    && dnf install -y freetype freetype-devel \
    && dnf clean all \
    && rm -rf /var/cache/dnf

# 设置额外参数
EXPOSE 80
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENV JAVA_OPTS="\
 -Xms512m \
 -Xmx1024m"
CMD java ${JAVA_OPTS} -Duser.timezone=Asia/Shanghai -jar app.jar