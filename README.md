SemWeb2NL
=========

### About
Semantic Web related concepts converted to Natural language

### Structure

SemWeb2NL currently consists of the following modules:

1. [Triple2NL](https://github.com/AKSW/SemWeb2NL/wiki/Triple2NL) - Convert triples into natural language
2. [SPARQL2NL](https://github.com/AKSW/SemWeb2NL/wiki/SPARQL2NL) - Convert SPARQL queries into natural language
3. [AVATAR](https://github.com/AKSW/SemWeb2NL/wiki/AVATAR) - Entity summarization
4. [ASSESS](https://github.com/AKSW/SemWeb2NL/wiki/ASSESS)  - Automatic Self Assessment
 
### Docker
If you want to use Docker, build the assess-service.war file.
Copy the .war file and the Dockerfile into a new folder and run

`sudo docker build -t assess-demo-backend .`

to build the image.

To run the image: 

`sudo docker run -d --restart=always --name assess-demo-backend  -p 9902:8080 assess-demo-backend`
