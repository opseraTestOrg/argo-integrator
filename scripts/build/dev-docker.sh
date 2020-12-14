#!/bin/bash
set -xe
docker build -t opsera-argo-integrator:kube-dev  ../../
docker run --rm \
        440953937617.dkr.ecr.us-east-2.amazonaws.com/kubectl \
        aws ecr get-login-password \
        --region us-east-2 \
        | docker login --username AWS \
        --password-stdin 440953937617.dkr.ecr.us-east-2.amazonaws.com

date_tag=`date +%-d-%m-%Y-%T | sed 's/:/-/g'`
docker tag opsera-argo-integrator:kube-dev 440953937617.dkr.ecr.us-east-2.amazonaws.com/opsera-argo-integrator:kube-dev
docker tag opsera-argo-integrator:kube-dev 440953937617.dkr.ecr.us-east-2.amazonaws.com/opsera-argo-integrator:kube-dev-${date_tag}

docker push 440953937617.dkr.ecr.us-east-2.amazonaws.com/opsera-argo-integrator:kube-dev
docker push 440953937617.dkr.ecr.us-east-2.amazonaws.com/opsera-argo-integrator:kube-dev-${date_tag}


