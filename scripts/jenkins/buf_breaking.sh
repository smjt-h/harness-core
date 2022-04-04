#!/bin/bash 
git fetch origin $1
git checkout $1
git status 
EXIT_CODE=0
output=""
IFS=$'\n' bufOut=($(buf breaking --against '.git#branch=develop' ))
for m in $(git log develop..$1 --format=format:%H )
do
    echo $m
    for k in $(git show --pretty="" --name-only $m | grep \.proto$)
    do         
        echo $k
        
        for l in "${bufOut[@]}"
        do
            if grep -q "$k" <<< "$l";
            then
                output+=$l
                output+="\n"
                echo $l
                EXIT_CODE=100
            fi
        done
    done  
done   
if [ -z "$output" ]
then
  output="No Breaking Change"
fi 
echo $EXIT_CODE >> exit_code.txt
echo $output >> output.txt          