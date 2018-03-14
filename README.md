Make sure $JAVA_HOME is pointing to the location where java is installed. Make sure maven is correctly exported.

OPTIONAL - To set up maven, run following commands:
export JAVA_HOME=$(/usr/libexec/java_home)
export PATH=$JAVA_HOME/jre/bin:$PATH
export PATH=~/ReplaceThisWithMavenFolderLocation/bin:$PATH

Note:
• The program is tested on personal computer with maven 3.3.9 and on lectura with maven 2.2.1.
• This program uses Lucene for language processing (dependencies already added to pom.xml).

!!!Detailed instructions in ProjectReport.pdf!!!