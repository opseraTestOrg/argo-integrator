curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
mv kustomize /usr/local/bin
git clone ${GIT_URL} --depth 1 --branch ${GIT_BRANCH}
cd ${GIT_FILE_PATH}
kustomize edit set image ${IMAGE_REFERENCE}=${IMAGE_URL}
git status
git config --global user.email ${GIT_USERNAME}
git config --global user.name ${GIT_USERNAME}
git add ${GIT_PATH_FILE_NAME}
git commit -m "updating the kustomization yaml"
git push origin ${GIT_BRANCH}