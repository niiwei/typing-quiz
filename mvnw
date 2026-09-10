#!/bin/sh
# Maven Wrapper startup script for Unix

# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

if [ -z "$JAVA_HOME" ] ; then
  if [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME="`/usr/libexec/java_home`"
  fi
fi

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD="`which java`"
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." >&2
  echo "  We cannot execute $JAVACMD" >&2
  exit 1
fi

# Find the project base dir
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$PWD}"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

# Download wrapper if not exists
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar"
    
    if command -v curl > /dev/null; then
        curl -o "$WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v wget > /dev/null; then
        wget -O "$WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "Error: curl or wget required to download Maven Wrapper"
        exit 1
    fi
fi

# Execute Maven
exec "$JAVACMD" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  $WRAPPER_LAUNCHER "$@"
