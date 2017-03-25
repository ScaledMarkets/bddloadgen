# Set up project environment.

export M2_HOME=/usr/local/apache-maven/apache-maven-3.2.3
export M2=$M2_HOME/bin
export MAVEN_OPTS="-Xms256m -Xmx512m"
alias maven="${M2}/mvn"
export JAVA_HOME=`/usr/libexec/java_home`
