#!/bin/bash
set -xe

clusername=${1:-opsera-test-cluster}
servicename=${2:-opsera-argo-integrator }

mkdir -p  ${PWD}/.tmp/kube-vol

docker run --rm --network host \
        -v ${PWD}/.tmp/kube-vol:/root/.kube \
        440953937617.dkr.ecr.us-east-2.amazonaws.com/kubectl \
        aws --profile opsera-test ecr get-login-password \
        --region us-east-2 \
        | docker login --username AWS \
        --password-stdin 440953937617.dkr.ecr.us-east-2.amazonaws.com

docker pull 440953937617.dkr.ecr.us-east-2.amazonaws.com/kubectl:latest

docker run --rm --network host \
        -v ${PWD}/.tmp/kube-vol:/root/.kube \
        440953937617.dkr.ecr.us-east-2.amazonaws.com/kubectl \
        aws --profile opsera-test eks update-kubeconfig --name ${clusername}

docker run --rm --name kubectl \
        -v ${PWD}/.tmp/kube-vol:/root/.kube \
        440953937617.dkr.ecr.us-east-2.amazonaws.com/kubectl:latest \
        kubectl delete pods -n microservices -l app.kubernetes.io/name=${servicename}

docker run --rm --name kubectl \
        -v ${PWD}/.tmp/kube-vol:/root/.kube \
        440953937617.dkr.ecr.us-east-2.amazonaws.com/kubectl:latest \
        kubectl get pod -n microservices -l app.kubernetes.io/name=${servicename}

rm -rf  ${PWD}/.tmp/kube-vol
