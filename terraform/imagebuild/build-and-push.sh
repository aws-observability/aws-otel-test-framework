#! /bin/bash



# login ecr


# build
docker build -t ${image_name} ${build_path}

# push
docker push