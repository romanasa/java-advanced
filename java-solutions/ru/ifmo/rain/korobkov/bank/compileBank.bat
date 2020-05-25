
echo "Compiling"

mkdir _build
set ADD=../../../../..
set LIB=%ADD%/../lib
set CP=%LIB%/junit-jupiter-api-5.4.2.jar;%LIB%/apiguardian-api-1.0.0.jar
javac -cp %CP% *.java -d _build 