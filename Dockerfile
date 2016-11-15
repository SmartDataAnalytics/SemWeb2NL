FROM tomcat:8
COPY assess-service.war /usr/local/tomcat/webapps/
WORKDIR /usr/local/tomcat
EXPOSE 8080
#CMD ["mvn", "clean",  "tomcat:run",  "-Dmaven.tomcat.port=8080", "-Dmaven.test.skip=true"]
#CMD ["mvn", "clean",  "tomcat:run-war",  "-Dmaven.tomcat.port=8080"]
CMD ["catalina.sh", "run"]
