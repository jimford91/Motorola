# Running the application

Ensure that the "upload" directory has already been created in the root of the project. 

To run the project simply navigate within an IDE such as Intellij and run the MotorolaApplicationTests class. 
Alternatively

# Design Choices



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
to keep it in the FileServiceImpl (even though this sort of does break the "Singulary of Concerns" principle)