# Running the application

Ensure that the "upload" directory has already been created in the root of the project. 

To run the project simply navigate within an IDE such as Intellij and run the MotorolaApplicationTests class.

To use your own OpenAI account for the documentation generation replace the "spring.ai.openai.api-key" property in appliation.yml
with your own api key. Currently it is set up to my one for this project.

To view the api in Swagger go to: http://localhost:8080/swagger-ui/index.html while the application is running.

# Design Choices

I decided to go with a local file system for ease of setting up and implementation while structuring the codebase in a way
that implementing a different store would be easy and follow the "Open/Closed Principle".

I decided to implement a simple regex for filenames to show that the consideration had been made, but more time would be needed to 
refine the regex and I felt the time was better spent elsewhere. 

I decided to go with no authentication for the endpoints, as that can be added on and I thought it was best to work on the endpoints
and assume no authentication was needed. 

For the locking mechanism I decided to use the "Lock" functionality introduced in Java 8 and use a basic map to only lock on filenames.
This simple locking mechanism assumes that files can be read multiple times but cannot combine the operations of "delete" and "update"
with other operations themselves or read operations.

I chose Swagger to use as the API documentation generation as it was I am comfortable with.

For the metrics, I have not added a key but the ability is there to plug in a datadog key. 
I did not have time to look into this part of the exercise and even though I have had usage with datadog integrated with services, 
I did not do the initial integration.

# Improvements

Various improvements could be made to the application given more time, some of these improvements are:

Instead of using a local filestore use an external storage location, such as an S3 bucket instead.
To implement this change, the main change would be to add another implementation to the FileRepository interface that 
would have the sole purpose of interacting with S3.

Another improvement is around security of the files. The current filename restrictions are rather strict and could be expanded
into a more complex Regex to watch for harmful patterns specifically instead of just excluding dangerous characters. 

Also along security could be checking for harmful content within the files themselves. 

A performance based improvement that could be made would be instead of traversing the directory for a list of the files,
store the metadata in a database or even something with quicker access like a redis cache or ElasticSearch index. 
This would give a larger performance improvement especially then there are lots of files and possibly lots of directories.

The locking mechanism could be extracted into its own service for reuse and more isolated testing, however due to time I decided
to keep it in the FileServiceImpl (even though this sort of does break the "Singulary of Concerns" principle).

Testing wise, I have not fully tested everything due to time but the e2e tests demonstrate my knowledge around that area. 
The unit tests cover the FileController and FileService which covers various validation checks aswell as the locking mechanism. 
The FileServiceImplTest does not cover all possible scenarios with the lock across all methods, however it does catch the 
key and edge scenarios across a couple of the methods. Implementing these methods would have just been a "Copy Paste Change"
job and to save time I decided to skip them.

There is very limited Javadoccing. I was focused on getting the solution done but usually I would add more documentation, both
inline where needed and javadoccing on methods.

After doing some reading while trying to fix tests, I came upon the FileLock functionality within the NIO library within Java. 
Locking at the file level with the access to the file would be better than locking within the java code instead how I implemented it.